import java.io.File;
import java.util.ArrayList;


public class JackAnalyzer {
	
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
            System.out.println("Incorrect Usage");
        }
        else{
            String fInputName = args[0];
            File fInput = new File(fInputName);
            String fOutPath = "";
            String tFileOutPath = "";
            File fOutput,tOutput;
            ArrayList<File> jFiles = new ArrayList<File>();
            
            if (fInput.isFile()) {
                String path = fInput.getAbsolutePath();

                if (!path.endsWith(".jack")) {
                    throw new IllegalArgumentException("Needs to be a .jack");
                }
                jFiles.add(fInput);
            } 
            else if (fInput.isDirectory()) {
                jFiles = getJackFiles(fInput);
                
                if (jFiles.size() == 0) {
                    throw new IllegalArgumentException("No Jack Files Found");
                }
            }

            for (File f: jFiles) {
                fOutPath = f.getAbsolutePath().substring(0, f.getAbsolutePath().lastIndexOf(".")) + ".xml";
                tFileOutPath = f.getAbsolutePath().substring(0, f.getAbsolutePath().lastIndexOf(".")) + "T.xml";
                fOutput = new File(fOutPath);
                tOutput = new File(tFileOutPath);
                CompilationEngine compilationEngine = new CompilationEngine(f,fOutput,tOutput);
                compilationEngine.compileClass();
                System.out.println("File created : " + fOutPath);
                System.out.println("File created : " + tFileOutPath);
            }

        }

    }
}