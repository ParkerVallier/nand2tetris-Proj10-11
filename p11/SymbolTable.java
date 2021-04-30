import java.util.HashMap;
import java.util.Map;

public class SymbolTable {

    private HashMap<String,Symbol> classSymbols;
    private HashMap<String,Symbol> subroutineSymbols;
    private HashMap<Symbol.KIND,Integer> indices;


    public SymbolTable() {
        classSymbols = new HashMap<String, Symbol>();
        subroutineSymbols = new HashMap<String, Symbol>();

        indices = new HashMap<Symbol.KIND, Integer>();
        indices.put(Symbol.KIND.ARG,0);
        indices.put(Symbol.KIND.FIELD,0);
        indices.put(Symbol.KIND.STATIC,0);
        indices.put(Symbol.KIND.VAR,0);

    }


    public void startSubroutine(){
        subroutineSymbols.clear();
        indices.put(Symbol.KIND.VAR,0);
        indices.put(Symbol.KIND.ARG,0);
    }


    public void define(String name, String type, Symbol.KIND kind){

        if (kind == Symbol.KIND.ARG || kind == Symbol.KIND.VAR){

            int index = indices.get(kind);
            Symbol symbol = new Symbol(type,kind,index);
            indices.put(kind,index+1);
            subroutineSymbols.put(name,symbol);

        }else if(kind == Symbol.KIND.STATIC || kind == Symbol.KIND.FIELD){

            int index = indices.get(kind);
            Symbol symbol = new Symbol(type,kind,index);
            indices.put(kind,index+1);
            classSymbols.put(name,symbol);

        }

    }

    public int varCount(Symbol.KIND kind){
        return indices.get(kind);
    }

    public Symbol.KIND kindOf(String name){

        Symbol symbol = lookUp(name);

        if (symbol != null) return symbol.getKind();

        return Symbol.KIND.NONE;
    }

    public String typeOf(String name){

        Symbol symbol = lookUp(name);

        if (symbol != null) return symbol.getType();

        return "";
    }

    public int indexOf(String name){

        Symbol symbol = lookUp(name);

        if (symbol != null) return symbol.getIndex();

        return -1;
    }

    private Symbol lookUp(String name){

        if (classSymbols.get(name) != null){
            return classSymbols.get(name);
        }else if (subroutineSymbols.get(name) != null){
            return subroutineSymbols.get(name);
        }else {
            return null;
        }

    }

}