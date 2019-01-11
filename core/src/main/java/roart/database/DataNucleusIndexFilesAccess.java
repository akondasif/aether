package roart.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import roart.common.constants.EurekaConstants;

public class DataNucleusIndexFilesAccess extends IndexFilesAccess {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    public String getAppName() {
	return EurekaConstants.DATANUCLEUS;
    }

}
