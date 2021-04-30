import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class JackTokenizer {

    public static enum TYPE {KEYWORD, SYMBOL, IDENTIFIER, INT_CONST, STRING_CONST, NONE};

    public static enum KEYWORD {CLASS, METHOD,FUNCTION,CONSTRUCTOR, INT,BOOLEAN,CHAR,VOID, VAR,STATIC,FIELD, LET,DO,IF,ELSE,WHILE, RETURN, TRUE,FALSE,NULL, THIS};

    private String curToken;
    private TYPE curTokenType;
    private int pointer;
    private ArrayList<String> tokens;

    private static Pattern tokenPatterns;
    private static String keyWordReg;
    private static String symbolReg;
    private static String intReg;
    private static String strReg;
    private static String idReg;

    private static HashMap<String,KEYWORD> keyWordMap = new HashMap<String, KEYWORD>();
    private static HashSet<Character> opSet = new HashSet<Character>();

    static {

        keyWordMap.put("class",KEYWORD.CLASS);keyWordMap.put("constructor",KEYWORD.CONSTRUCTOR);keyWordMap.put("function",KEYWORD.FUNCTION);
        keyWordMap.put("method",KEYWORD.METHOD);keyWordMap.put("field",KEYWORD.FIELD);keyWordMap.put("static",KEYWORD.STATIC);
        keyWordMap.put("var",KEYWORD.VAR);keyWordMap.put("int",KEYWORD.INT);keyWordMap.put("char",KEYWORD.CHAR);
        keyWordMap.put("boolean",KEYWORD.BOOLEAN);keyWordMap.put("void",KEYWORD.VOID);keyWordMap.put("true",KEYWORD.TRUE);
        keyWordMap.put("false",KEYWORD.FALSE);keyWordMap.put("null",KEYWORD.NULL);keyWordMap.put("this",KEYWORD.THIS);
        keyWordMap.put("let",KEYWORD.LET);keyWordMap.put("do",KEYWORD.DO);keyWordMap.put("if",KEYWORD.IF);
        keyWordMap.put("else",KEYWORD.ELSE);keyWordMap.put("while",KEYWORD.WHILE);keyWordMap.put("return",KEYWORD.RETURN);

        opSet.add('+');opSet.add('-');opSet.add('*');opSet.add('/');opSet.add('&');opSet.add('|');
        opSet.add('<');opSet.add('>');opSet.add('=');
    }


    public JackTokenizer(File inFile) {

        try {

            Scanner scan = new Scanner(inFile);
            String preprocessed = "";
            String line = "";
            while(scan.hasNext()){
                line = noComments(scan.nextLine()).trim();

                if (line.length() > 0) {
                    preprocessed += line + "\n";
                }
            }

            preprocessed = noBlockComments(preprocessed).trim();

            initRegs();
            Matcher m = tokenPatterns.matcher(preprocessed);
            tokens = new ArrayList<String>();
            pointer = 0;

            while (m.find()){

                tokens.add(m.group());

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();

        }
        curToken = "";
        curTokenType = TYPE.NONE;

    }

    private void initRegs(){
        keyWordReg = "";

        for (String seg: keyWordMap.keySet()){

            keyWordReg += seg + "|";

        }

        symbolReg = "[\\&\\*\\+\\(\\)\\.\\/\\,\\-\\]\\;\\~\\}\\|\\{\\>\\=\\[\\<]";
        intReg = "[0-9]+";
        strReg = "\"[^\"\n]*\"";
        idReg = "[a-zA-Z_]\\w*";

        tokenPatterns = Pattern.compile(idReg + "|" + keyWordReg + symbolReg + "|" + intReg + "|" + strReg);
    }

    public boolean hasMoreTokens() {
        return pointer < tokens.size();
    }

    public void advance(){

        if (hasMoreTokens()) {
            curToken = tokens.get(pointer);
            pointer++;
        }
        else {
            throw new IllegalStateException("No more tokens");
        }

        if (curToken.matches(keyWordReg)){
            curTokenType = TYPE.KEYWORD;
        }
        else if (curToken.matches(symbolReg)){
            curTokenType = TYPE.SYMBOL;
        }
        else if (curToken.matches(intReg)){
            curTokenType = TYPE.INT_CONST;
        }
        else if (curToken.matches(strReg)){
            curTokenType = TYPE.STRING_CONST;
        }
        else if (curToken.matches(idReg)){
            curTokenType = TYPE.IDENTIFIER;
        }
        else {

            throw new IllegalArgumentException("Unknown token:" + curToken);
        }

    }

    public String getCurrentToken() {
        return curToken;
    }

    public TYPE tokenType(){

        return curTokenType;
    }

    public KEYWORD keyWord(){
        if (curTokenType == TYPE.KEYWORD){
            return keyWordMap.get(curToken);
        }
        else {
            throw new IllegalStateException("Not keyword");
        }
    }


    public char symbol(){
        if (curTokenType == TYPE.SYMBOL){
            return curToken.charAt(0);
        }
        else{
            throw new IllegalStateException("Not Symbol");
        }
    }

    public String identifier(){
        if (curTokenType == TYPE.IDENTIFIER){
            return curToken;
        }
        else {
            throw new IllegalStateException("Current token is not an identifier! current type:" + curTokenType);
        }
    }

    public int intVal(){
        if(curTokenType == TYPE.INT_CONST){
            return Integer.parseInt(curToken);
        }
        else {
            throw new IllegalStateException("Current token is not an integer constant!");
        }
    }

    public String stringVal(){
        if (curTokenType == TYPE.STRING_CONST){
            return curToken.substring(1, curToken.length() - 1);
        }
        else {
            throw new IllegalStateException("Current token is not a string constant!");
        }
    }

    public void pointerBack(){
        if (pointer > 0) {
            pointer--;
            curToken = tokens.get(pointer);
        }
    }

    public boolean isOp(){
        return opSet.contains(symbol());
    }

    public static String noComments(String strIn){
        int position = strIn.indexOf("//");
        if (position != -1){
            strIn = strIn.substring(0, position);
        }
        return strIn;
    }


    public static String noSpaces(String strIn){
        String result = "";
        if (strIn.length() != 0){
            String[] segs = strIn.split(" ");
            for (String s: segs){
                result += s;
            }
        }
        return result;
    }

    public static String noBlockComments(String strIn){
        int startIndex = strIn.indexOf("/*");
        if (startIndex == -1) return strIn;
        String result = strIn;
        int endIndex = strIn.indexOf("*/");
        
        while(startIndex != -1){
            if (endIndex == -1){
                return strIn.substring(0,startIndex - 1);
            }
            result = result.substring(0,startIndex) + result.substring(endIndex + 2);
            startIndex = result.indexOf("/*");
            endIndex = result.indexOf("*/");
        }
        return result;
    }
}