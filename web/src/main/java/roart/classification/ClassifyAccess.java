package roart.classification;

import roart.common.machinelearning.MachineLearningClassifyParam;
import roart.common.machinelearning.MachineLearningClassifyResult;
import roart.common.machinelearning.MachineLearningConstructorParam;
import roart.common.machinelearning.MachineLearningConstructorResult;
import roart.common.machinelearning.MachineLearningResult;
import roart.common.searchengine.SearchEngineIndexResult;
import roart.config.MyConfig;
import roart.model.ResultItem;
import roart.service.ControlService;
import roart.util.EurekaConstants;
import roart.util.EurekaUtil;

import java.util.List;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ClassifyAccess {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    public abstract String getAppName();

    public String constructor() {
        MachineLearningConstructorParam param = new MachineLearningConstructorParam();
        param.nodename = ControlService.nodename;
        param.conf = MyConfig.conf;
        MachineLearningConstructorResult result = EurekaUtil.sendMe(MachineLearningConstructorResult.class, param, getAppName(), EurekaConstants.CONSTRUCTOR);   	
        return result.error;
    }
    
    public String deconstructor() {
        MachineLearningConstructorParam param = new MachineLearningConstructorParam();
        param.nodename = ControlService.nodename;
        param.conf = MyConfig.conf;
        MachineLearningConstructorResult result = EurekaUtil.sendMe(MachineLearningConstructorResult.class, param, getAppName(), EurekaConstants.DECONSTRUCTOR);   	
        return result.error;
    }
    
    public String classify(String type, String language) {
    	MachineLearningClassifyParam param = new MachineLearningClassifyParam();
        param.nodename = ControlService.nodename;
        param.conf = MyConfig.conf;
    	param.str = type;
    	param.language = language;
        MachineLearningClassifyResult result = EurekaUtil.sendMe(MachineLearningClassifyResult.class, param, getAppName(), EurekaConstants.CLASSIFY);

        if (result == null) {
        	return null;
        }
        
        return result.result;
    }

}

