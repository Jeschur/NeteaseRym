package demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Utils{
    
    
    public static List<String> getFileLines(String fileName){
        List<String> lines = new ArrayList<String>();
        File file = new File(fileName);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            int lineCount = 0;
            while ((tempString = reader.readLine()) != null) {
                try{
                    lineCount++;
                    lines.add(tempString.trim());
                } catch (Exception e) {
                    System.out.println("parse line {} failed: {}" + lineCount + tempString);
                    throw e;
                }
            }
            System.out.println("file Line is :"+lineCount);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        
        return lines;
    }
    
}