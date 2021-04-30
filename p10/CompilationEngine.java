import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class CompilationEngine {

	// Fields
	private PrintWriter pWriter;
    private PrintWriter tPrintWriter;
    private JackTokenizer jTokenizer;

    public CompilationEngine(File inFile, File outFile, File outTokenFile) {
        try {
            jTokenizer = new JackTokenizer(inFile);
            pWriter = new PrintWriter(outFile);
            tPrintWriter = new PrintWriter(outTokenFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void compileType(){
        jTokenizer.advance();
        boolean isType = false;

        if (jTokenizer.tokenType() == JackTokenizer.KEYWORD && (jTokenizer.keyWord() == JackTokenizer.INT || jTokenizer.keyWord() == JackTokenizer.CHAR || jTokenizer.keyWord() == JackTokenizer.BOOLEAN)){
            pWriter.print("<keyword>" + jTokenizer.getCurrentToken() + "</keyword>\n");
            tPrintWriter.print("<keyword>" + jTokenizer.getCurrentToken() + "</keyword>\n");
            isType = true;
        }
        if (jTokenizer.tokenType() == JackTokenizer.IDENTIFIER){
            pWriter.print("<identifier>" + jTokenizer.identifier() + "</identifier>\n");
            tPrintWriter.print("<identifier>" + jTokenizer.identifier() + "</identifier>\n");
            isType = true;
        }
        if (!isType) error("in|char|boolean|className");
    }

    public void compileClass(){

        // Class
        jTokenizer.advance();

        if (jTokenizer.tokenType() != JackTokenizer.KEYWORD || jTokenizer.keyWord() != JackTokenizer.CLASS){
            error("class");
        }

        pWriter.print("<class>\n");
        tPrintWriter.print("<tokens>\n");

        pWriter.print("<keyword>class</keyword>\n");
        tPrintWriter.print("<keyword>class</keyword>\n");

        // Class names
        jTokenizer.advance();

        if (jTokenizer.tokenType() != JackTokenizer.IDENTIFIER){
            error("className");
        }

        pWriter.print("<identifier>" + jTokenizer.identifier() + "</identifier>\n");
        tPrintWriter.print("<identifier>" + jTokenizer.identifier() + "</identifier>\n");

        // {
        requireSymbol('{');
        
        compileClassVarDec();
        compileSubroutine();

        // }
        requireSymbol('}');

        if (jTokenizer.hasMoreTokens()){
            throw new IllegalStateException("Unexpected tokens");
        }

        tPrintWriter.print("</tokens>\n");
        pWriter.print("</class>\n");

        pWriter.close();
        tPrintWriter.close();

    }
    
    private void compileClassVarDec(){

        jTokenizer.advance();

        // }
        if (jTokenizer.tokenType() == JackTokenizer.SYMBOL && jTokenizer.symbol() == '}'){
            jTokenizer.pointerBack();
            return;
        }

        if (jTokenizer.tokenType() != JackTokenizer.KEYWORD){
            error("Keywords");
        }

        if (jTokenizer.keyWord() == JackTokenizer.CONSTRUCTOR || jTokenizer.keyWord() == JackTokenizer.FUNCTION || jTokenizer.keyWord() == JackTokenizer.METHOD){
            jTokenizer.pointerBack();
            return;
        }

        pWriter.print("<classVarDec>\n");

        if (jTokenizer.keyWord() != JackTokenizer.STATIC && jTokenizer.keyWord() != JackTokenizer.FIELD){
            error("static or field");
        }

        pWriter.print("<keyword>" + jTokenizer.getCurrentToken() + "</keyword>\n");
        tPrintWriter.print("<keyword>" + jTokenizer.getCurrentToken() + "</keyword>\n");

        compileType();

        do {

            jTokenizer.advance();
            if (jTokenizer.tokenType() != JackTokenizer.IDENTIFIER){
                error("identifier");
            }

            pWriter.print("<identifier>" + jTokenizer.identifier() + "</identifier>\n");
            tPrintWriter.print("<identifier>" + jTokenizer.identifier() + "</identifier>\n");

            // Handle , or ;
            jTokenizer.advance();

            if (jTokenizer.tokenType() != JackTokenizer.SYMBOL || (jTokenizer.symbol() != ',' && jTokenizer.symbol() != ';')){
                error("',' or ';'");
            }

            if (jTokenizer.symbol() == ','){

                pWriter.print("<symbol>,</symbol>\n");
                tPrintWriter.print("<symbol>,</symbol>\n");
            }
            else {
                pWriter.print("<symbol>;</symbol>\n");
                tPrintWriter.print("<symbol>;</symbol>\n");
                break;
            }

        }while(true);
        pWriter.print("</classVarDec>\n");
        compileClassVarDec();
    }

    private void compileSubroutine(){
        jTokenizer.advance();

        if (jTokenizer.tokenType() == JackTokenizer.SYMBOL && jTokenizer.symbol() == '}'){
            jTokenizer.pointerBack();
            return;
        }

        if (jTokenizer.tokenType() != JackTokenizer.KEYWORD || (jTokenizer.keyWord() != JackTokenizer.CONSTRUCTOR && jTokenizer.keyWord() != JackTokenizer.FUNCTION && jTokenizer.keyWord() != JackTokenizer.METHOD)){
            error("constructor|function|method");
        }

        pWriter.print("<subroutineDec>\n");
        pWriter.print("<keyword>" + jTokenizer.getCurrentToken() + "</keyword>\n");
        tPrintWriter.print("<keyword>" + jTokenizer.getCurrentToken() + "</keyword>\n");

        jTokenizer.advance();
        if (jTokenizer.tokenType() == JackTokenizer.KEYWORD && jTokenizer.keyWord() == JackTokenizer.VOID){
            pWriter.print("<keyword>void</keyword>\n");
            tPrintWriter.print("<keyword>void</keyword>\n");
        }else {
            jTokenizer.pointerBack();
            compileType();
        }

        jTokenizer.advance();
        if (jTokenizer.tokenType() != JackTokenizer.IDENTIFIER){
            error("subroutineName");
        }

        pWriter.print("<identifier>" + jTokenizer.identifier() + "</identifier>\n");
        tPrintWriter.print("<identifier>" + jTokenizer.identifier() + "</identifier>\n");

        requireSymbol('(');

        pWriter.print("<parameterList>\n");
        compileParameterList();
        pWriter.print("</parameterList>\n");

        requireSymbol(')');

        compileSubroutineBody();

        pWriter.print("</subroutineDec>\n");

        compileSubroutine();

    }


    private void compileSubroutineBody(){
        pWriter.print("<subroutineBody>\n");

        requireSymbol('{');

        compileVarDec();

        pWriter.print("<statements>\n");
        compileStatement();
        pWriter.print("</statements>\n");

        requireSymbol('}');
        pWriter.print("</subroutineBody>\n");
    }

    private void compileStatement(){


        jTokenizer.advance();

        if (jTokenizer.tokenType() == JackTokenizer.SYMBOL && jTokenizer.symbol() == '}'){
            jTokenizer.pointerBack();
            return;
        }

        if (jTokenizer.tokenType() != JackTokenizer.KEYWORD){
            error("keyword");
        }else {
            switch (jTokenizer.keyWord()){
                case JackTokenizer.LET:compileLet();break;
                case JackTokenizer.IF:compileIf();break;
                case JackTokenizer.WHILE:compilesWhile();break;
                case JackTokenizer.DO:compileDo();break;
                case JackTokenizer.RETURN:compileReturn();break;
                default:error("'let'|'if'|'while'|'do'|'return'");
            }
        }

        compileStatement();
    }

    private void compileParameterList(){

        jTokenizer.advance();
        if (jTokenizer.tokenType() == JackTokenizer.SYMBOL && jTokenizer.symbol() == ')'){
            jTokenizer.pointerBack();
            return;
        }

        jTokenizer.pointerBack();
        do {

            compileType();


            jTokenizer.advance();
            if (jTokenizer.tokenType() != JackTokenizer.IDENTIFIER){
                error("identifier");
            }
             pWriter.print("<identifier>" + jTokenizer.identifier() + "</identifier>\n");
            tPrintWriter.print("<identifier>" + jTokenizer.identifier() + "</identifier>\n");

            jTokenizer.advance();
            if (jTokenizer.tokenType() != JackTokenizer.SYMBOL || (jTokenizer.symbol() != ',' && jTokenizer.symbol() != ')')){
                error("',' or ')'");
            }

            if (jTokenizer.symbol() == ','){
                pWriter.print("<symbol>,</symbol>\n");
                tPrintWriter.print("<symbol>,</symbol>\n");
            }else {
                jTokenizer.pointerBack();
                break;
            }

        }while(true);

    }

    private void compileVarDec(){
        jTokenizer.advance();

        if (jTokenizer.tokenType() != JackTokenizer.KEYWORD || jTokenizer.keyWord() != JackTokenizer.VAR){
            jTokenizer.pointerBack();
            return;
        }

        pWriter.print("<varDec>\n");

        pWriter.print("<keyword>var</keyword>\n");
        tPrintWriter.print("<keyword>var</keyword>\n");

        compileType();
        do {
            jTokenizer.advance();

            if (jTokenizer.tokenType() != JackTokenizer.IDENTIFIER){
                error("identifier");
            }

            pWriter.print("<identifier>" + jTokenizer.identifier() + "</identifier>\n");
            tPrintWriter.print("<identifier>" + jTokenizer.identifier() + "</identifier>\n");
            jTokenizer.advance();

            if (jTokenizer.tokenType() != JackTokenizer.SYMBOL || (jTokenizer.symbol() != ',' && jTokenizer.symbol() != ';')){
                error("',' or ';'");
            }

            if (jTokenizer.symbol() == ','){
                pWriter.print("<symbol>,</symbol>\n");
                tPrintWriter.print("<symbol>,</symbol>\n");

            }
            else {
                pWriter.print("<symbol>;</symbol>\n");
                tPrintWriter.print("<symbol>;</symbol>\n");
                break;
            }


        }while(true);
        pWriter.print("</varDec>\n");
        compileVarDec();

    }

    private void compileDo(){
        pWriter.print("<doStatement>\n");

        pWriter.print("<keyword>do</keyword>\n");
        tPrintWriter.print("<keyword>do</keyword>\n");
        compileSubroutineCall();
        requireSymbol(';');

        pWriter.print("</doStatement>\n");
    }

    private void compileLet(){
        pWriter.print("<letStatement>\n");
        pWriter.print("<keyword>let</keyword>\n");
        tPrintWriter.print("<keyword>let</keyword>\n");
        jTokenizer.advance();
        
        if (jTokenizer.tokenType() != JackTokenizer.IDENTIFIER){
            error("varName");
        }

        pWriter.print("<identifier>" + jTokenizer.identifier() + "</identifier>\n");
        tPrintWriter.print("<identifier>" + jTokenizer.identifier() + "</identifier>\n");
        jTokenizer.advance();
        
        if (jTokenizer.tokenType() != JackTokenizer.SYMBOL || (jTokenizer.symbol() != '[' && jTokenizer.symbol() != '=')){
            error("'['|'='");
        }

        boolean expExist = false;
        if (jTokenizer.symbol() == '['){
            expExist = true;
            pWriter.print("<symbol>[</symbol>\n");
            tPrintWriter.print("<symbol>[</symbol>\n");
            compileExpression();

            jTokenizer.advance();
            if (jTokenizer.tokenType() == JackTokenizer.SYMBOL && jTokenizer.symbol() == ']'){
                pWriter.print("<symbol>]</symbol>\n");
                tPrintWriter.print("<symbol>]</symbol>\n");
            }
            else {
                error("']'");
            }
        }

        if (expExist) jTokenizer.advance();

        pWriter.print("<symbol>=</symbol>\n");
        tPrintWriter.print("<symbol>=</symbol>\n");
        compileExpression();
        requireSymbol(';');
        pWriter.print("</letStatement>\n");
    }

    private void compilesWhile(){
        pWriter.print("<whileStatement>\n");
        pWriter.print("<keyword>while</keyword>\n");
        tPrintWriter.print("<keyword>while</keyword>\n");
        requireSymbol('(');
        compileExpression();
        requireSymbol(')');
        requireSymbol('{');
        pWriter.print("<statements>\n");
        compileStatement();
        pWriter.print("</statements>\n");
        requireSymbol('}');
        pWriter.print("</whileStatement>\n");
    }


    private void compileReturn(){
        pWriter.print("<returnStatement>\n");
        pWriter.print("<keyword>return</keyword>\n");
        tPrintWriter.print("<keyword>return</keyword>\n");
        jTokenizer.advance();
        if (jTokenizer.tokenType() == JackTokenizer.SYMBOL && jTokenizer.symbol() == ';'){
            pWriter.print("<symbol>;</symbol>\n");
            tPrintWriter.print("<symbol>;</symbol>\n");
            pWriter.print("</returnStatement>\n");
            return;
        }

        jTokenizer.pointerBack();
        compileExpression();
        requireSymbol(';');
        pWriter.print("</returnStatement>\n");
    }

    private void compileIf(){
        pWriter.print("<ifStatement>\n");

        pWriter.print("<keyword>if</keyword>\n");
        tPrintWriter.print("<keyword>if</keyword>\n");
        requireSymbol('(');
        compileExpression();
        requireSymbol(')');
        requireSymbol('{');
        pWriter.print("<statements>\n");
        compileStatement();
        pWriter.print("</statements>\n");
        requireSymbol('}');

        jTokenizer.advance();
        if (jTokenizer.tokenType() == JackTokenizer.KEYWORD && jTokenizer.keyWord() == JackTokenizer.ELSE){
            pWriter.print("<keyword>else</keyword>\n");
            tPrintWriter.print("<keyword>else</keyword>\n");
            requireSymbol('{');
            pWriter.print("<statements>\n");
            compileStatement();
            pWriter.print("</statements>\n");
            requireSymbol('}');
        }
        else {
            jTokenizer.pointerBack();
        }

        pWriter.print("</ifStatement>\n");

    }

    private void compileTerm(){
        pWriter.print("<term>\n");

        jTokenizer.advance();
        if (jTokenizer.tokenType() == JackTokenizer.IDENTIFIER){
            String tempId = jTokenizer.identifier();

            jTokenizer.advance();
            if (jTokenizer.tokenType() == JackTokenizer.SYMBOL && jTokenizer.symbol() == '['){
                pWriter.print("<identifier>" + tempId + "</identifier>\n");
                tPrintWriter.print("<identifier>" + tempId + "</identifier>\n");
                pWriter.print("<symbol>[</symbol>\n");
                tPrintWriter.print("<symbol>[</symbol>\n");
                compileExpression();
                requireSymbol(']');
                
            }
            else if (jTokenizer.tokenType() == JackTokenizer.SYMBOL && (jTokenizer.symbol() == '(' || jTokenizer.symbol() == '.')){
                jTokenizer.pointerBack();jTokenizer.pointerBack();
                compileSubroutineCall();
            }
            else {
                pWriter.print("<identifier>" + tempId + "</identifier>\n");
                tPrintWriter.print("<identifier>" + tempId + "</identifier>\n");
                jTokenizer.pointerBack();
            }

        }
        else{
            if (jTokenizer.tokenType() == JackTokenizer.INT_CONST){
                pWriter.print("<integerConstant>" + jTokenizer.intVal() + "</integerConstant>\n");
                tPrintWriter.print("<integerConstant>" + jTokenizer.intVal() + "</integerConstant>\n");
            }
            else if (jTokenizer.tokenType() == JackTokenizer.STRING_CONST){
                pWriter.print("<stringConstant>" + jTokenizer.stringVal() + "</stringConstant>\n");
                tPrintWriter.print("<stringConstant>" + jTokenizer.stringVal() + "</stringConstant>\n");
            }
            else if(jTokenizer.tokenType() == JackTokenizer.KEYWORD && (jTokenizer.keyWord() == JackTokenizer.TRUE || jTokenizer.keyWord() == JackTokenizer.FALSE || jTokenizer.keyWord() == JackTokenizer.NULL || jTokenizer.keyWord() == JackTokenizer.THIS)){
                    pWriter.print("<keyword>" + jTokenizer.getCurrentToken() + "</keyword>\n");
                    tPrintWriter.print("<keyword>" + jTokenizer.getCurrentToken() + "</keyword>\n");
            }
            else if (jTokenizer.tokenType() == JackTokenizer.SYMBOL && jTokenizer.symbol() == '('){
                pWriter.print("<symbol>(</symbol>\n");
                tPrintWriter.print("<symbol>(</symbol>\n");
                compileExpression();
                requireSymbol(')');
            }
            else if (jTokenizer.tokenType() == JackTokenizer.SYMBOL && (jTokenizer.symbol() == '-' || jTokenizer.symbol() == '~')){
                pWriter.print("<symbol>" + jTokenizer.symbol() + "</symbol>\n");
                tPrintWriter.print("<symbol>" + jTokenizer.symbol() + "</symbol>\n");
                compileTerm();
            }
            else {
                error("integerConstant|stringConstant|keywordConstant|'(' expression ')'|unaryOp term");
            }
        }
        pWriter.print("</term>\n");
    }

    private void compileSubroutineCall(){
        jTokenizer.advance();
        
        if (jTokenizer.tokenType() != JackTokenizer.IDENTIFIER){
            error("identifier");
        }

        pWriter.print("<identifier>" + jTokenizer.identifier() + "</identifier>\n");
        tPrintWriter.print("<identifier>" + jTokenizer.identifier() + "</identifier>\n");
        jTokenizer.advance();
        
        if (jTokenizer.tokenType() == JackTokenizer.SYMBOL && jTokenizer.symbol() == '('){
            pWriter.print("<symbol>(</symbol>\n");
            tPrintWriter.print("<symbol>(</symbol>\n");
            pWriter.print("<expressionList>\n");
            compileExpressionList();
            pWriter.print("</expressionList>\n");
            requireSymbol(')');
        }
        else if (jTokenizer.tokenType() == JackTokenizer.SYMBOL && jTokenizer.symbol() == '.'){
            pWriter.print("<symbol>.</symbol>\n");
            tPrintWriter.print("<symbol>.</symbol>\n");
            jTokenizer.advance();
            if (jTokenizer.tokenType() != JackTokenizer.IDENTIFIER){
                error("identifier");
            }
            pWriter.print("<identifier>" + jTokenizer.identifier() + "</identifier>\n");
            tPrintWriter.print("<identifier>" + jTokenizer.identifier() + "</identifier>\n");
            requireSymbol('(');
            pWriter.print("<expressionList>\n");
            compileExpressionList();
            pWriter.print("</expressionList>\n");
            requireSymbol(')');
        }
        else {
            error("'('|'.'");
        }
    }

    private void compileExpression(){
        pWriter.print("<expression>\n");
        compileTerm();
        do {
            jTokenizer.advance();
            if (jTokenizer.tokenType() == JackTokenizer.SYMBOL && jTokenizer.isOp()){
                if (jTokenizer.symbol() == '>'){
                    pWriter.print("<symbol>&gt;</symbol>\n");
                    tPrintWriter.print("<symbol>&gt;</symbol>\n");
                }
                else if (jTokenizer.symbol() == '<'){
                    pWriter.print("<symbol>&lt;</symbol>\n");
                    tPrintWriter.print("<symbol>&lt;</symbol>\n");
                }
                else if (jTokenizer.symbol() == '&') {
                    pWriter.print("<symbol>&amp;</symbol>\n");
                    tPrintWriter.print("<symbol>&amp;</symbol>\n");
                }
                else {
                    pWriter.print("<symbol>" + jTokenizer.symbol() + "</symbol>\n");
                    tPrintWriter.print("<symbol>" + jTokenizer.symbol() + "</symbol>\n");
                }
                compileTerm();
            }
            else {
                jTokenizer.pointerBack();
                break;
            }

        }while (true);
        pWriter.print("</expression>\n");
    }

    private void compileExpressionList(){
        jTokenizer.advance();
        if (jTokenizer.tokenType() == JackTokenizer.SYMBOL && jTokenizer.symbol() == ')'){
            jTokenizer.pointerBack();
        }
        else {
            jTokenizer.pointerBack();
            compileExpression();
            do {
                jTokenizer.advance();
                if (jTokenizer.tokenType() == JackTokenizer.SYMBOL && jTokenizer.symbol() == ','){
                    pWriter.print("<symbol>,</symbol>\n");
                    tPrintWriter.print("<symbol>,</symbol>\n");
                    compileExpression();
                }
                else {
                    jTokenizer.pointerBack();
                    break;
                }
            }while (true);
        }
    }

    private void error(String val){
        throw new IllegalStateException("Expected token missing : " + val + " Current token:" + jTokenizer.getCurrentToken());
    }

    private void requireSymbol(char symbol){
        jTokenizer.advance();
        if (jTokenizer.tokenType() == JackTokenizer.SYMBOL && jTokenizer.symbol() == symbol){
            pWriter.print("<symbol>" + symbol + "</symbol>\n");
            tPrintWriter.print("<symbol>" + symbol + "</symbol>\n");
        }else {
            error("'" + symbol + "'");
        }
    }
}