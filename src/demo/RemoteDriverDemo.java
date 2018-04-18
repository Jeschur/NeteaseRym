package demo;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;


public class RemoteDriverDemo{
    public static void main(String[] args){ 
        RemoteWebDriver remoteWebDriver = null;
        DesiredCapabilities caps = DesiredCapabilities.android("00daaf096545a096"); //需指定要运行的手机deviceID
        try {
            remoteWebDriver = new RemoteWebDriver(new URL("http://127.0.0.1:4444/wd/hub"), caps);//需要指定手机爬虫服务器地址
            remoteWebDriver.get("com.google.android.calculator"); //操作的APP名字
            System.out.println("start to sleep");
            TimeUnit.SECONDS.sleep(3);
            remoteWebDriver.findElementByXPath("com.google.android.calculator:id/digit_5").click();
            System.out.println("start to screenshotSystem");
            String screenshotPath = remoteWebDriver.screenshotSystem(); //截屏操作，返回截屏图片的NOS地址
            System.out.println("screenshotPath:" + screenshotPath);            
            System.out.println("start to closeAPP");
            TimeUnit.SECONDS.sleep(1);
            remoteWebDriver.closeAPP("com.google.android.calculator"); //关闭APP
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            try {
                if(remoteWebDriver!=null) remoteWebDriver.quit();//关闭session
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}