import java.io.File;

public class CompilationEngine {

    private VMWriter vmWriter;
    private JackTokenizer jTokenizer;
    private SymbolTable symbolTable;
    private String currentClass;
    private String currentSubroutine;

    private int labelIndex;

    public CompilationEngine(File inFile, File outFile) {
        jTokenizer = new JackTokenizer(inFile);
        vmWriter = new VMWriter(outFile);
        symbolTable = new SymbolTable();
        labelIndex = 0;
    }

    private String currentFunction(){
        if (currentClass.length() != 0 && currentSubroutine.length() !=0){
            return currentClass + "." + currentSubroutine;
        }
        return "";
    }

    private String compileType(){
        jTokenizer.advance();
        if (jTokenizer.tokenType() == JackTokenizer.TYPE.KEYWORD && (jTokenizer.keyWord() == JackTokenizer.KEYWORD.INT || jTokenizer.keyWord() == JackTokenizer.KEYWORD.CHAR || jTokenizer.keyWord() == JackTokenizer.KEYWORD.BOOLEAN)){
            return jTokenizer.getCurrentToken();
        }
        if (jTokenizer.tokenType() == JackTokenizer.TYPE.IDENTIFIER){
            return jTokenizer.identifier();
        }
        error("in|char|boolean|className");
        return "";
    }

    public void compileClass(){
        jTokenizer.advance();

        if (jTokenizer.tokenType() != JackTokenizer.TYPE.KEYWORD || jTokenizer.keyWord() != JackTokenizer.KEYWORD.CLASS){
            System.out.println(jTokenizer.getCurrentToken());
            error("class");
        }

        jTokenizer.advance();

        if (jTokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER){
            error("className");
        }

        currentClass = jTokenizer.identifier();

        requireSymbol('{');
        compileClassVarDec();
        compileSubroutine();
        requireSymbol('}');

        if (jTokenizer.hasMoreTokens()){
            throw new IllegalStateException("Unexpected tokens");
        }

        vmWriter.close();

    }

    private void compileClassVarDec(){

        jTokenizer.advance();

        if (jTokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && jTokenizer.symbol() == '}'){
            jTokenizer.pointerBack();
            return;
        }

        if (jTokenizer.tokenType() != JackTokenizer.TYPE.KEYWORD){
            error("Keywords");
        }

        if (jTokenizer.keyWord() == JackTokenizer.KEYWORD.CONSTRUCTOR || jTokenizer.keyWord() == JackTokenizer.KEYWORD.FUNCTION || jTokenizer.keyWord() == JackTokenizer.KEYWORD.METHOD){
            jTokenizer.pointerBack();
            return;
        }

        if (jTokenizer.keyWord() != JackTokenizer.KEYWORD.STATIC && jTokenizer.keyWord() != JackTokenizer.KEYWORD.FIELD){
            error("static or field");
        }

        Symbol.KIND kind = null;
        String type = "";
        String name = "";

        switch (jTokenizer.keyWord()){
            case STATIC:kind = Symbol.KIND.STATIC;break;
            case FIELD:kind = Symbol.KIND.FIELD;break;
        }

        type = compileType();

        boolean varNamesDone = false;

        do {

            jTokenizer.advance();
            if (jTokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER){
                error("identifier");
            }

            name = jTokenizer.identifier();
            symbolTable.define(name,type,kind);
            jTokenizer.advance();

            if (jTokenizer.tokenType() != JackTokenizer.TYPE.SYMBOL || (jTokenizer.symbol() != ',' && jTokenizer.symbol() != ';')){
                error("',' or ';'");
            }

            if (jTokenizer.symbol() == ';'){
                break;
            }


        }while(true);

        compileClassVarDec();
    }

    private void compileSubroutine(){

        jTokenizer.advance();

        if (jTokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && jTokenizer.symbol() == '}'){
            jTokenizer.pointerBack();
            return;
        }

        if (jTokenizer.tokenType() != JackTokenizer.TYPE.KEYWORD || (jTokenizer.keyWord() != JackTokenizer.KEYWORD.CONSTRUCTOR && jTokenizer.keyWord() != JackTokenizer.KEYWORD.FUNCTION && jTokenizer.keyWord() != JackTokenizer.KEYWORD.METHOD)){
            error("constructor|function|method");
        }

        JackTokenizer.KEYWORD keyword = jTokenizer.keyWord();
        symbolTable.startSubroutine();

        if (jTokenizer.keyWord() == JackTokenizer.KEYWORD.METHOD){
            symbolTable.define("this",currentClass, Symbol.KIND.ARG);
        }

        String type = "";

        jTokenizer.advance();
        if (jTokenizer.tokenType() == JackTokenizer.TYPE.KEYWORD && jTokenizer.keyWord() == JackTokenizer.KEYWORD.VOID){
            type = "void";
        }else {
            jTokenizer.pointerBack();
            type = compileType();
        }

        jTokenizer.advance();
        if (jTokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER){
            error("subroutineName");
        }

        currentSubroutine = jTokenizer.identifier();

        requireSymbol('(');

        compileParameterList();

        requireSymbol(')');

        compileSubroutineBody(keyword);

        compileSubroutine();

    }

    private void compileSubroutineBody(JackTokenizer.KEYWORD keyword){
        requireSymbol('{');
        compileVarDec();
        wrtieFunctionDec(keyword);
        compileStatement();
        requireSymbol('}');
    }

    private void wrtieFunctionDec(JackTokenizer.KEYWORD keyword){

        vmWriter.writeFunction(currentFunction(),symbolTable.varCount(Symbol.KIND.VAR));

        if (keyword == JackTokenizer.KEYWORD.METHOD){
            vmWriter.writePush(VMWriter.SEGMENT.ARG, 0);
            vmWriter.writePop(VMWriter.SEGMENT.POINTER,0);

        }
        else if (keyword == JackTokenizer.KEYWORD.CONSTRUCTOR){
            vmWriter.writePush(VMWriter.SEGMENT.CONST,symbolTable.varCount(Symbol.KIND.FIELD));
            vmWriter.writeCall("Memory.alloc", 1);
            vmWriter.writePop(VMWriter.SEGMENT.POINTER,0);
        }
    }

    private void compileStatement(){

        jTokenizer.advance();

        if (jTokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && jTokenizer.symbol() == '}'){
            jTokenizer.pointerBack();
            return;
        }

        if (jTokenizer.tokenType() != JackTokenizer.TYPE.KEYWORD){
            error("keyword");
        }
        else {
            switch (jTokenizer.keyWord()){
                case LET:compileLet();break;
                case IF:compileIf();break;
                case WHILE:compilesWhile();break;
                case DO:compileDo();break;
                case RETURN:compileReturn();break;
                default:error("'let'|'if'|'while'|'do'|'return'");
            }
        }

        compileStatement();
    }

    private void compileParameterList(){

        jTokenizer.advance();
        if (jTokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && jTokenizer.symbol() == ')'){
            jTokenizer.pointerBack();
            return;
        }

        String type = "";

        jTokenizer.pointerBack();
        do {
            type = compileType();

            jTokenizer.advance();
            if (jTokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER){
                error("identifier");
            }

            symbolTable.define(jTokenizer.identifier(),type, Symbol.KIND.ARG);

            jTokenizer.advance();
            if (jTokenizer.tokenType() != JackTokenizer.TYPE.SYMBOL || (jTokenizer.symbol() != ',' && jTokenizer.symbol() != ')')){
                error("',' or ')'");
            }

            if (jTokenizer.symbol() == ')'){
                jTokenizer.pointerBack();
                break;
            }

        }while(true);

    }

    private void compileVarDec(){

        jTokenizer.advance();

        if (jTokenizer.tokenType() != JackTokenizer.TYPE.KEYWORD || jTokenizer.keyWord() != JackTokenizer.KEYWORD.VAR){
            jTokenizer.pointerBack();
            return;
        }

        String type = compileType();

        boolean varNamesDone = false;

        do {

            jTokenizer.advance();

            if (jTokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER){
                error("identifier");
            }

            symbolTable.define(jTokenizer.identifier(),type, Symbol.KIND.VAR);
            jTokenizer.advance();

            if (jTokenizer.tokenType() != JackTokenizer.TYPE.SYMBOL || (jTokenizer.symbol() != ',' && jTokenizer.symbol() != ';')){
                error("',' or ';'");
            }

            if (jTokenizer.symbol() == ';'){
                break;
            }


        }while(true);
        compileVarDec();

    }

    private void compileDo(){
        compileSubroutineCall();
        requireSymbol(';');
        vmWriter.writePop(VMWriter.SEGMENT.TEMP,0);
    }

    private void compileLet(){
        jTokenizer.advance();
        
        if (jTokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER){
            error("varName");
        }

        String varName = jTokenizer.identifier();
        jTokenizer.advance();
        
        if (jTokenizer.tokenType() != JackTokenizer.TYPE.SYMBOL || (jTokenizer.symbol() != '[' && jTokenizer.symbol() != '=')){
            error("'['|'='");
        }

        boolean expExist = false;

        if (jTokenizer.symbol() == '['){
            expExist = true;
            vmWriter.writePush(getSeg(symbolTable.kindOf(varName)),symbolTable.indexOf(varName));
            compileExpression();
            requireSymbol(']');
            vmWriter.writeArithmetic(VMWriter.COMMAND.ADD);
        }

        if (expExist) jTokenizer.advance();

        compileExpression();
        requireSymbol(';');

        if (expExist){
            vmWriter.writePop(VMWriter.SEGMENT.TEMP,0);
            vmWriter.writePop(VMWriter.SEGMENT.POINTER,1);
            vmWriter.writePush(VMWriter.SEGMENT.TEMP,0);
            vmWriter.writePop(VMWriter.SEGMENT.THAT,0);
        }
        else {
            vmWriter.writePop(getSeg(symbolTable.kindOf(varName)), symbolTable.indexOf(varName));

        }
    }

    private VMWriter.SEGMENT getSeg(Symbol.KIND kind){

        switch (kind){
            case FIELD:return VMWriter.SEGMENT.THIS;
            case STATIC:return VMWriter.SEGMENT.STATIC;
            case VAR:return VMWriter.SEGMENT.LOCAL;
            case ARG:return VMWriter.SEGMENT.ARG;
            default:return VMWriter.SEGMENT.NONE;
        }

    }

    private void compilesWhile(){

        String continueLabel = newLabel();
        String topLabel = newLabel();

        vmWriter.writeLabel(topLabel);

        requireSymbol('(');
        compileExpression();
        requireSymbol(')');
        vmWriter.writeArithmetic(VMWriter.COMMAND.NOT);
        vmWriter.writeIf(continueLabel);
        requireSymbol('{');
        compileStatement();
        requireSymbol('}');
        vmWriter.writeGoto(topLabel);
        vmWriter.writeLabel(continueLabel);
    }

    private String newLabel(){
        return "LABEL_" + (labelIndex++);
    }

    private void compileReturn(){

        jTokenizer.advance();

        if (jTokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && jTokenizer.symbol() == ';'){
            vmWriter.writePush(VMWriter.SEGMENT.CONST,0);
        }else {
            jTokenizer.pointerBack();
            compileExpression();
            requireSymbol(';');
        }

        vmWriter.writeReturn();

    }

    private void compileIf(){

        String elseLabel = newLabel();
        String endLabel = newLabel();

        requireSymbol('(');
        compileExpression();
        requireSymbol(')');
        vmWriter.writeArithmetic(VMWriter.COMMAND.NOT);
        vmWriter.writeIf(elseLabel);
        requireSymbol('{');
        compileStatement();
        requireSymbol('}');
        vmWriter.writeGoto(endLabel);
        vmWriter.writeLabel(elseLabel);
        jTokenizer.advance();
        
        if (jTokenizer.tokenType() == JackTokenizer.TYPE.KEYWORD && jTokenizer.keyWord() == JackTokenizer.KEYWORD.ELSE){
            requireSymbol('{');
            compileStatement();
            requireSymbol('}');
        }
        else {
            jTokenizer.pointerBack();
        }

        vmWriter.writeLabel(endLabel);

    }

    private void compileTerm(){
        jTokenizer.advance();
        
        if (jTokenizer.tokenType() == JackTokenizer.TYPE.IDENTIFIER){
            String tempId = jTokenizer.identifier();
            jTokenizer.advance();
            
            if (jTokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && jTokenizer.symbol() == '['){
                vmWriter.writePush(getSeg(symbolTable.kindOf(tempId)),symbolTable.indexOf(tempId));
                compileExpression();
                requireSymbol(']');
                vmWriter.writeArithmetic(VMWriter.COMMAND.ADD);
                vmWriter.writePop(VMWriter.SEGMENT.POINTER,1);
                vmWriter.writePush(VMWriter.SEGMENT.THAT,0);
            }
            else if (jTokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && (jTokenizer.symbol() == '(' || jTokenizer.symbol() == '.')){
                jTokenizer.pointerBack();jTokenizer.pointerBack();
                compileSubroutineCall();
            }
            else {
                jTokenizer.pointerBack();
                vmWriter.writePush(getSeg(symbolTable.kindOf(tempId)), symbolTable.indexOf(tempId));
            }

        }
        else{
            if (jTokenizer.tokenType() == JackTokenizer.TYPE.INT_CONST){
                vmWriter.writePush(VMWriter.SEGMENT.CONST,jTokenizer.intVal());
            }
            else if (jTokenizer.tokenType() == JackTokenizer.TYPE.STRING_CONST){
                String str = jTokenizer.stringVal();

                vmWriter.writePush(VMWriter.SEGMENT.CONST,str.length());
                vmWriter.writeCall("String.new",1);

                for (int i = 0; i < str.length(); i++){
                    vmWriter.writePush(VMWriter.SEGMENT.CONST,(int)str.charAt(i));
                    vmWriter.writeCall("String.appendChar",2);
                }

            }
            else if(jTokenizer.tokenType() == JackTokenizer.TYPE.KEYWORD && jTokenizer.keyWord() == JackTokenizer.KEYWORD.TRUE){
                vmWriter.writePush(VMWriter.SEGMENT.CONST,0);
                vmWriter.writeArithmetic(VMWriter.COMMAND.NOT);

            }
            else if(jTokenizer.tokenType() == JackTokenizer.TYPE.KEYWORD && jTokenizer.keyWord() == JackTokenizer.KEYWORD.THIS){
                vmWriter.writePush(VMWriter.SEGMENT.POINTER,0);

            }
            else if(jTokenizer.tokenType() == JackTokenizer.TYPE.KEYWORD && (jTokenizer.keyWord() == JackTokenizer.KEYWORD.FALSE || jTokenizer.keyWord() == JackTokenizer.KEYWORD.NULL)){
                vmWriter.writePush(VMWriter.SEGMENT.CONST,0);
            }
            else if (jTokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && jTokenizer.symbol() == '('){
                compileExpression();
                requireSymbol(')');
            }
            else if (jTokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && (jTokenizer.symbol() == '-' || jTokenizer.symbol() == '~')){

                char s = jTokenizer.symbol();
                compileTerm();
                if (s == '-'){
                    vmWriter.writeArithmetic(VMWriter.COMMAND.NEG);
                }
                else {
                    vmWriter.writeArithmetic(VMWriter.COMMAND.NOT);
                }

            }
            else {
                error("integerConstant|stringConstant|keywordConstant|'(' expression ')'|unaryOp term");
            }
        }

    }


    private void compileSubroutineCall(){
        jTokenizer.advance();
        
        if (jTokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER){
            error("identifier");
        }

        String name = jTokenizer.identifier();
        int nArgs = 0;
        jTokenizer.advance();
        
        if (jTokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && jTokenizer.symbol() == '('){
            vmWriter.writePush(VMWriter.SEGMENT.POINTER,0);
            nArgs = compileExpressionList() + 1;
            requireSymbol(')');
            vmWriter.writeCall(currentClass + '.' + name, nArgs);

        }
        else if (jTokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && jTokenizer.symbol() == '.'){
            String objName = name;
            jTokenizer.advance();
            
            if (jTokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER){
                error("identifier");
            }

            name = jTokenizer.identifier();
            String type = symbolTable.typeOf(objName);

            if (type.equals("int")||type.equals("boolean")||type.equals("char")||type.equals("void")){
                error("no built-in type");
            }
            else if (type.equals("")){
                name = objName + "." + name;
            }
            else {
                nArgs = 1;
                vmWriter.writePush(getSeg(symbolTable.kindOf(objName)), symbolTable.indexOf(objName));
                name = symbolTable.typeOf(objName) + "." + name;
            }

            requireSymbol('(');
            nArgs += compileExpressionList();
            requireSymbol(')');
            vmWriter.writeCall(name,nArgs);
        }
        else {
            error("'('|'.'");
        }

    }

    private void compileExpression(){
        compileTerm();
        do {
            jTokenizer.advance();
            
            if (jTokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && jTokenizer.isOp()){

                String opCmd = "";

                switch (jTokenizer.symbol()){
                    case '+':opCmd = "add";break;
                    case '-':opCmd = "sub";break;
                    case '*':opCmd = "call Math.multiply 2";break;
                    case '/':opCmd = "call Math.divide 2";break;
                    case '<':opCmd = "lt";break;
                    case '>':opCmd = "gt";break;
                    case '=':opCmd = "eq";break;
                    case '&':opCmd = "and";break;
                    case '|':opCmd = "or";break;
                    default:error("Unknown op!");
                }

                compileTerm();

                vmWriter.writeCommand(opCmd,"","");

            }
            else {
                jTokenizer.pointerBack();
                break;
            }

        }while (true);

    }

    private int compileExpressionList(){
        int nArgs = 0;
        jTokenizer.advance();
        
        if (jTokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && jTokenizer.symbol() == ')'){
            jTokenizer.pointerBack();
        }
        else {
            nArgs = 1;
            jTokenizer.pointerBack();
            compileExpression();
            do {
                jTokenizer.advance();
                if (jTokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && jTokenizer.symbol() == ','){
                    compileExpression();
                    nArgs++;
                }
                else {
                    jTokenizer.pointerBack();
                    break;
                }
            }while (true);
        }
        return nArgs;
    }

    private void error(String val){
        throw new IllegalStateException("Expected token missing : " + val + " Current token:" + jTokenizer.getCurrentToken());
    }

    private void requireSymbol(char symbol){
        jTokenizer.advance();
        if (jTokenizer.tokenType() != JackTokenizer.TYPE.SYMBOL || jTokenizer.symbol() != symbol){
            error("'" + symbol + "'");
        }
    }
}