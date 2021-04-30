import java.io.File;
import java.util.ArrayList;

public class JackCompiler {

    public static ArrayList<File> getJackFiles(File dir){
        File[] file = dir.listFiles();
        ArrayList<File> result = new ArrayList<File>();
        
        if (file == null) return result;
        
        for (File f:file){
        	
            if (f.getName().endsWith(".jack")){
                result.add(f);
            }
        }
        return result;
    }

    public static void main(String[] args) {
        if (args.length != 1){
            System.out.println("Usage:java JackCompiler [filename|directory]");
        }
        else {
            String fInputName = args[0];
            File fInput = new File(fInputName);
            String fOutPath = "";
            File fOutput;
            ArrayList<File> jFiles = new ArrayList<File>();

            if (fInput.isFile()) {

                String path = fInput.getAbsolutePath();
                
                if (!path.endsWith(".jack")) {
                    throw new IllegalArgumentException(".jack only");

                }
                jFiles.add(fInput);
            }
            else if (fInput.isDirectory()) {

                jFiles = getJackFiles(fInput);

                if (jFiles.size() == 0) {
                    throw new IllegalArgumentException("No Jack Found");
                }
            }

            for (File f: jFiles) {
                fOutPath = f.getAbsolutePath().substring(0, f.getAbsolutePath().lastIndexOf(".")) + ".vm";
                fOutput = new File(fOutPath);
                CompilationEngine compilationEngine = new CompilationEngine(f,fOutput);
                compilationEngine.compileClass();
                System.out.println("File created : " + fOutPath);
            }

        }

    }

}