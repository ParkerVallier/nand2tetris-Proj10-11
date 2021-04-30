import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class JackTokenizer {

    // Types
    public final static int KEYWORD = 1;
    public final static int SYMBOL = 2;
    public final static int IDENTIFIER = 3;
    public final static int INT_CONST = 4;
    public final static int STRING_CONST = 5;

    // Keywords
    public final static int CLASS = 10;
    public final static int METHOD = 11;
    public final static int FUNCTION = 12;
    public final static int CONSTRUCTOR = 13;
    public final static int INT = 14;
    public final static int BOOLEAN = 15;
    public final static int CHAR = 16;
    public final static int VOID = 17;
    public final static int VAR = 18;
    public final static int STATIC = 19;
    public final static int FIELD = 20;
    public final static int LET = 21;
    public final static int DO = 22;
    public final static int IF = 23;
    public final static int ELSE = 24;
    public final static int WHILE = 25;
    public final static int RETURN = 26;
    public final static int TRUE = 27;
    public final static int FALSE = 28;
    public final static int NULL = 29;
    public final static int THIS = 30;

    private Scanner scan;
    private String curToken;
    private int curTokenType;
    private int pointer;
    private ArrayList<String> tokens;
    private static Pattern tokenPatterns;
    private static String keyWordReg;
    private static String symbolReg;
    private static String intReg;
    private static String strReg;
    private static String idReg;

    private static HashMap<String,Integer> keyMap = new HashMap<String, Integer>();
    private static HashSet<Character> opSet = new HashSet<Character>();

    static {

        keyMap.put("class",CLASS);keyMap.put("constructor",CONSTRUCTOR);keyMap.put("function",FUNCTION);
        keyMap.put("method",METHOD);keyMap.put("field",FIELD);keyMap.put("static",STATIC);
        keyMap.put("var",VAR);keyMap.put("int",INT);keyMap.put("char",CHAR);
        keyMap.put("boolean",BOOLEAN);keyMap.put("void",VOID);keyMap.put("true",TRUE);
        keyMap.put("false",FALSE);keyMap.put("null",NULL);keyMap.put("this",THIS);
        keyMap.put("let",LET);keyMap.put("do",DO);keyMap.put("if",IF);
        keyMap.put("else",ELSE);keyMap.put("while",WHILE);keyMap.put("return",RETURN);

        opSet.add('+');opSet.add('-');opSet.add('*');opSet.add('/');opSet.add('&');opSet.add('|');
        opSet.add('<');opSet.add('>');opSet.add('=');
    }

    public JackTokenizer(File inFile) {
        try {
            scan = new Scanner(inFile);
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
        curTokenType = -1;

    }

    private void initRegs(){
        keyWordReg = "";
        for (String seg: keyMap.keySet()){
            keyWordReg += seg + "|";
        }
        symbolReg = "[\\&\\*\\+\\(\\)\\.\\/\\,\\-\\]\\;\\~\\}\\|\\{\\>\\=\\[\\<]";
        intReg = "[0-9]+";
        strReg = "\"[^\"\n]*\"";
        idReg = "[\\w_]+";
        tokenPatterns = Pattern.compile(keyWordReg + symbolReg + "|" + intReg + "|" + strReg + "|" + idReg);
    }

    public boolean hasMoreTokens() {
        return pointer < tokens.size();
    }

    public void advance(){
        if (hasMoreTokens()) {
            curToken = tokens.get(pointer);
            pointer++;
        }else {
            throw new IllegalStateException("Out of Tokens");
        }

        if (curToken.matches(keyWordReg)){
            curTokenType = KEYWORD;
        }
        else if (curToken.matches(symbolReg)){
            curTokenType = SYMBOL;
        }
        else if (curToken.matches(intReg)){
            curTokenType = INT_CONST;
        }
        else if (curToken.matches(strReg)){
            curTokenType = STRING_CONST;
        }
        else if (curToken.matches(idReg)){
            curTokenType = IDENTIFIER;
        }
        else {
            throw new IllegalArgumentException("Unknown token:" + curToken);
        }
    }

    public String getCurrentToken() {
        return curToken;
    }

    public int tokenType(){
        return curTokenType;
    }

    public int keyWord(){
        if (curTokenType == KEYWORD){
            return keyMap.get(curToken);
        }
        else {
            throw new IllegalStateException("Not keyword");
        }
    }

    public char symbol(){
        if (curTokenType == SYMBOL){
            return curToken.charAt(0);
        }
        else{
            throw new IllegalStateException("Not symbol");
        }
    }

    public String identifier(){
        if (curTokenType == IDENTIFIER){
            return curToken;
        }
        else {
            throw new IllegalStateException("Not identifier");
        }
    }

    public int intVal(){
        if(curTokenType == INT_CONST){
            return Integer.parseInt(curToken);
        }
        else {
            throw new IllegalStateException("Not constant");
        }
    }

    public String stringVal(){
        if (curTokenType == STRING_CONST){
            return curToken.substring(1, curToken.length() - 1);
        }
        else {
            throw new IllegalStateException("Not String contant");
        }
    }

    public void pointerBack(){
        if (pointer > 0) {
            pointer--;
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
        int sIndex = strIn.indexOf("/*");
        
        if (sIndex == -1) return strIn;
        String result = strIn;
        int endIndex = strIn.indexOf("*/");
        
        while(sIndex != -1){

            if (endIndex == -1){
                return strIn.substring(0,sIndex - 1);

            }
            result = result.substring(0,sIndex) + result.substring(endIndex + 2);
            sIndex = result.indexOf("/*");
            endIndex = result.indexOf("*/");
        }
        return result;
    }
}