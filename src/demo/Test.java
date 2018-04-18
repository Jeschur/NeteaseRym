package demo;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Test{
    
    public static void main(String[] args){ 
        try {
            Random r = new Random();   
            for(int i = 1; i < 4; i++){   
                int v = r.nextInt();
                System.out.println("第" + i + "次:" + v);
                assert v > 0;
            } 
        } catch (Exception e) {
            e.printStackTrace();
        }
          
    }   
}