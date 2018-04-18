package com.netease.mail.datasearch.crawler;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netease.mail.utils.Utils4FileRead;

/**
 * 只负责提交爬取任务，不负责检查数据生成等相关操作
 *
 * @author hzraoyaming
 *
 */
public class ResourceTasksSubmitOnly{
    private static final Logger logger = LoggerFactory.getLogger(ResourceTasksSubmitOnly.class);
    private static final String CURRENT_PATH = ResourceTasksSubmitOnly.class.getResource("/").getPath();
    static{
        PropertyConfigurator.configure(CURRENT_PATH + "log4j.properties");
    }
        
    public static void main(String[] args) {
        String urlsFileName = "urls.list";
        String curlUrl = "http://10.240.130.249:9090/resource-taobaoCategory/TaobaoCategoryServlet/";
        int threadNum = 20;
        if(args.length!=3) {
            System.out.println("java -jar ResourceTasksSubmitOnly.jar urlsFileName curlUrl threadNum");
            System.exit(-1);
        }
        else {
            urlsFileName = args[0];
            curlUrl = args[1];
            threadNum = Integer.parseInt(args[2]);
        }
        logger.info("urlsFileName:{}, curlUrl:{}, threadNum:{}", urlsFileName, curlUrl, threadNum);
        
        ResourceDataCheck.curlUrl = curlUrl;
        
        List<String> seeds = Utils4FileRead.convertFile2List(urlsFileName);
        logger.info("seeds size:{}", seeds.size());
        ExecutorService  excutor = Executors.newFixedThreadPool(threadNum);
        for(String seed:seeds) {
            ResourceDataCheck oneCheck = new ResourceDataCheck(seed, false);
            excutor.submit(oneCheck);
            try {
                TimeUnit.MILLISECONDS.sleep(5*1000);
            } catch (InterruptedException e) {}
        }
        logger.info("submit over!");
        excutor.shutdown();
        
    }

}