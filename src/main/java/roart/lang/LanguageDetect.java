package roart.lang;

import java.util.Date;
import java.util.ArrayList;
import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.Language;
import com.cybozu.labs.langdetect.LangDetectException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class LanguageDetect {

    private static Logger log = LoggerFactory.getLogger("LanguageDetect");

    private static boolean inited = false;

    public static void init(String profileDirectory) throws LangDetectException {
	DetectorFactory.loadProfile(profileDirectory);
	inited = true;
    }
    
    public static String detect(String text) {
	try {
	    if (!inited) init("./profiles/");
	    Date d = logstart();
	    Detector detector = DetectorFactory.create();
	    detector.append(text);
	    String retstr = detector.detect();
	    logstop(d);
	    log.info("language " + retstr);
	    log.info("language2 " + LanguageDetect.detectLangs(text));
	    return retstr;
	} catch (Exception e) {
	    log.error(Constants.EXCEPTION, e);
	} catch (Error e) {
	    log.fatal(Constants.ERROR, e);
	}
	return null;
    }

    public static ArrayList<Language> detectLangs(String text) throws LangDetectException {
	if (!inited) init("profiles");
	Detector detector = DetectorFactory.create();
	detector.append(text);
	return detector.getProbabilities();
    }

    public static Date logstart() {
        return new Date();
    }
    
    public static void logstop (Date d) {
        Date n = new Date();
        long m = n.getTime() - d.getTime();
        log.info("timerStop " + m);
    }

}
