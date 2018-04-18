package com.netease.mail.datasearch.crawler;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class NosCheckObservable extends Observable{
    private static final Logger logger = LoggerFactory.getLogger(NosCheckObservable.class);
    private static final String CURRENT_PATH = NosCheckObservable.class.getResource("/").getPath();
    static{
        PropertyConfigurator.configure(CURRENT_PATH + "log4j.properties");
        logger.info("load NosCheckObservable log4j.properties succeed");
    }
    
    public static String batchTime = null;
    public static String dataPath = null;
    
    private static ObjectMapper om = new ObjectMapper();
    private Set<String> checkSet;
    
    public NosCheckObservable(List<String> checkSet) {      
        this.checkSet = new HashSet<>(checkSet);
    }
    
    private void addList(List<String> checkSet) {
        this.checkSet.addAll(checkSet);
    }
    
    public static NosCheckObservable nosCheck = null;
    public static NosCheckObservable getInstance(List<String> checkSet) throws Exception {
        if (nosCheck == null) {
            nosCheck = new NosCheckObservable(checkSet);
        }
        else {
            nosCheck.addList(checkSet);
        }
        return nosCheck;
    }
    
    public Set<String> getCheckSet() {
        return checkSet;
    }
    
    public void updateInfo() {
        Set<String> finishedSet = new HashSet<>();
        for(String seed:checkSet) {
            String nosFname = DigestUtils.md5Hex(seed);
            try {
                NosData nosData = null;
                try {
                    nosData = om.readValue(NeteaseObjectStorage.download(nosFname), NosData.class);
                } catch (Exception e) {
                    continue;
                }
                
                String outFileName = dataPath + "/" + nosFname;
                PrintWriter pw = null;
                try {
                    pw = new PrintWriter(outFileName, "UTF-8");
                    pw.println(om.writeValueAsString(nosData));
                    pw.flush();
                } catch (Exception e) {
                    logger.info("write seed {} failed, e:", seed, e);
                    continue;
                }
                finally {
                    if(null!=pw)
                        try {
                            pw.close();
                        } catch (Exception e2) {}
                }
                logger.info("seed finished:{}, outFileName:{}", seed, outFileName);
                finishedSet.add(seed);
            } catch (Exception e) {
                logger.error("updateInfo error, seed:{} nosFname:{}, e", seed, nosFname, e);
                continue;
            }
        }
        checkSet.removeAll(finishedSet);
        logger.info("checkSet after:{} {}", checkSet.size(), checkSet);
        this.setChanged();
        this.notifyObservers();
        try {
            TimeUnit.SECONDS.sleep(30);
        } catch (Exception e) {}
    }
    
}