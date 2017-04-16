package roart.filesystem;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.web.bind.annotation.*;

import roart.config.NodeConfig;
import roart.util.EurekaConstants;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
//@EnableAutoConfiguration
@SpringBootApplication
@EnableDiscoveryClient
public abstract class FileSystemAbstractController {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private static Map<String, FileSystemOperations> operationMap = new HashMap();

	protected abstract FileSystemOperations createOperations(String nodename, NodeConfig nodeConf);

	private FileSystemOperations getOperations(String nodename, NodeConfig nodeConf) {
		FileSystemOperations operations = operationMap.get(nodename);
		if (operations == null) {
			operations = createOperations(nodename, nodeConf);
			operationMap.put(nodename, operations);
		}
		return operations;
	}

	@RequestMapping(value = "/" + EurekaConstants.CONSTRUCTOR,
			method = RequestMethod.POST)
	public FileSystemConstructorResult processConstructor(@RequestBody FileSystemConstructorParam param)
			throws Exception {
		String error = null;
		try {
			FileSystemOperations operations = getOperations(param.nodename, param.conf);
		} catch (Exception e) {
			log.error(roart.util.Constants.EXCEPTION, e);
			error = e.getMessage();
		}
		FileSystemConstructorResult result = new FileSystemConstructorResult();
		result.error = error;
		return result;
	}

	@RequestMapping(value = "/" + EurekaConstants.DESTRUCTOR,
			method = RequestMethod.POST)
	public FileSystemConstructorResult processDestructor(@RequestBody FileSystemConstructorParam param)
			throws Exception {
		FileSystemOperations operations = operationMap.remove(param.nodename);
		String error = null;
		if (operations != null) {
			try {
				operations.destroy();
			} catch (Exception e) {
				log.error(roart.util.Constants.EXCEPTION, e);
				error = e.getMessage();
			}
		} else {
			error = "did not exist";
		}
		FileSystemConstructorResult result = new FileSystemConstructorResult();
		result.error = error;
		return result;
	}

	@RequestMapping(value = "/" + EurekaConstants.LISTFILES,
			method = RequestMethod.POST)
	public FileSystemFileObjectResult processListFiles(@RequestBody FileSystemFileObjectParam param)
			throws Exception {
		FileSystemOperations operations = getOperations(param.nodename, param.conf);
		FileSystemFileObjectResult ret = operations.listFiles(param);
		return ret;
	}

	@RequestMapping(value = "/" + EurekaConstants.EXIST,
			method = RequestMethod.POST)
	public FileSystemBooleanResult processExist(@RequestBody FileSystemFileObjectParam param)
			throws Exception {
		FileSystemOperations operations = getOperations(param.nodename, param.conf);
		FileSystemBooleanResult ret = operations.exists(param);
		return ret;
	}

    @RequestMapping(value = "/" + EurekaConstants.GETABSOLUTEPATH,
            method = RequestMethod.POST)
    public FileSystemPathResult processGetAbsolutePath(@RequestBody FileSystemFileObjectParam param)
            throws Exception {
        FileSystemOperations operations = getOperations(param.nodename, param.conf);
        FileSystemPathResult ret = operations.getAbsolutePath(param);
        return ret;
    }

    @RequestMapping(value = "/" + EurekaConstants.ISDIRECTORY,
            method = RequestMethod.POST)
    public FileSystemBooleanResult processIsDirectory(@RequestBody FileSystemFileObjectParam param)
            throws Exception {
        FileSystemOperations operations = getOperations(param.nodename, param.conf);
        FileSystemBooleanResult ret = operations.isDirectory(param);
        return ret;
    }

    @RequestMapping(value = "/" + EurekaConstants.GETINPUTSTREAM,
            method = RequestMethod.POST)
    public FileSystemByteResult processGetInputStream(@RequestBody FileSystemFileObjectParam param)
            throws Exception {
        FileSystemOperations operations = getOperations(param.nodename, param.conf);
        FileSystemByteResult ret = operations.getInputStream(param);
        return ret;
    }

    @RequestMapping(value = "/" + EurekaConstants.GETPARENT,
            method = RequestMethod.POST)
    public FileSystemFileObjectResult processGetParent(@RequestBody FileSystemFileObjectParam param)
            throws Exception {
        FileSystemOperations operations = getOperations(param.nodename, param.conf);
        FileSystemFileObjectResult ret = operations.getParent(param);
        return ret;
    }

   @RequestMapping(value = "/" + EurekaConstants.GET,
            method = RequestMethod.POST)
    public FileSystemFileObjectResult processGet(@RequestBody FileSystemPathParam param)
            throws Exception {
        FileSystemOperations operations = getOperations(param.nodename, param.conf);
        FileSystemFileObjectResult ret = operations.get(param);
        return ret;
    }

	public static void main(String[] args) throws Exception {
		SpringApplication.run(FileSystemAbstractController.class, args);
	}
}