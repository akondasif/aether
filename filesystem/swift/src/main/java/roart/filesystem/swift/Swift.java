package roart.filesystem.swift;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import roart.common.config.ConfigConstants;
import roart.common.config.NodeConfig;
import roart.common.constants.Constants;
import roart.common.constants.FileSystemConstants;
import roart.common.filesystem.FileSystemBooleanResult;
import roart.common.filesystem.FileSystemByteResult;
import roart.common.filesystem.FileSystemConstructorResult;
import roart.common.filesystem.FileSystemFileObjectParam;
import roart.common.filesystem.FileSystemFileObjectResult;
import roart.common.filesystem.FileSystemMyFileResult;
import roart.common.filesystem.FileSystemPathParam;
import roart.common.filesystem.FileSystemPathResult;
import roart.common.filesystem.MyFile;
import roart.common.model.FileObject;
import roart.filesystem.FileSystemOperations;

import org.javaswift.joss.client.factory.AccountConfig;
import org.javaswift.joss.client.factory.AccountFactory;
import org.javaswift.joss.client.factory.AuthenticationMethod;
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.Directory;
import org.javaswift.joss.model.DirectoryOrObject;
import org.javaswift.joss.model.StoredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Swift extends FileSystemOperations {

    private static final Logger log = LoggerFactory.getLogger(Swift.class);

    /*private*/ SwiftConfig conf;

    private Map<String, DirectoryOrObject> dooMap = new HashMap<>();

    public Swift() {
    }

    public Swift(String nodename, NodeConfig nodeConf) {
        try {
            conf = new SwiftConfig();
            String url = nodeConf.getSwiftUrl();
            String username = nodeConf.getSwiftUser();
            String password = nodeConf.getSwiftKey();
            log.info("INFO " + url + " " + username + "  " + password);
            if (url != null) {
                AccountConfig config;
                config = new AccountConfig();
                config.setUsername( username);
                config.setPassword(password);
                config.setAuthUrl(url);
                config.setAuthenticationMethod(AuthenticationMethod.BASIC);
                Account account = new AccountFactory(config).createAccount();
                conf.account = account;
            }
        } catch (Exception e) {
            log.error("Exception", e);
            //return null;
        }
    }

    @Override
    public FileSystemFileObjectResult listFiles(FileSystemFileObjectParam param) {
        FileObject f = param.fo;
        List<FileObject> foList = new ArrayList<FileObject>();
        DirectoryOrObject mydir = dooMap.get(f.object);
        try {
            String containerName = param.conf.getSwiftContainer();
            Container container = conf.account.getContainer(containerName);
            if (mydir.isObject()) {
                foList.add(f);
            } else {
                Directory dir = mydir.getAsDirectory();
                Collection<DirectoryOrObject> list = container.listDirectory(dir);
                for (DirectoryOrObject doo : list) {
                    FileObject fo = new FileObject(doo.getName(), this.getClass().getSimpleName());
                    foList.add(fo);
                    dooMap.put(doo.getName(), doo);
                }
            }
            FileSystemFileObjectResult result = new FileSystemFileObjectResult();
            result.setFileObject(foList.toArray(new FileObject[0]));
            return result;
        } catch (Exception e) {
            log.error(Constants.EXCEPTION, e);
            return null;
        }
    }

    @Override
    public FileSystemMyFileResult listFilesFull(FileSystemFileObjectParam param) throws Exception {
        FileObject f = param.fo;
        Map<String, MyFile> map = new HashMap<>();
        DirectoryOrObject mydir = dooMap.get(f.object);
        try {
            String containerName = param.conf.getSwiftContainer();
            Container container = conf.account.getContainer(containerName);
            if (mydir.isObject()) {
                FileObject[] fo = new FileObject[1];
                fo[0] = f;
                MyFile my = getMyFile(containerName, fo, false);
                map.put(my.absolutePath, my);
            } else {
                Directory dir = mydir.getAsDirectory();
                Collection<DirectoryOrObject> list = container.listDirectory(dir);
                for (DirectoryOrObject doo : list) {
                    FileObject[] fo = new FileObject[1];
                    fo[0] = new FileObject(doo.getName(), this.getClass().getSimpleName());
                    MyFile my = getMyFile(containerName, fo, false);
                    map.put(my.absolutePath, my);
                    dooMap.put(doo.getName(), doo);
                }
            }
            FileSystemMyFileResult result = new FileSystemMyFileResult();
            result.map = map;
            return result;
        } catch (Exception e) {
            log.error(Constants.EXCEPTION, e);
            return null;
        }
    }

    @Override
    public FileSystemBooleanResult exists(FileSystemFileObjectParam param) {
        FileObject f = param.fo;
        DirectoryOrObject path = dooMap.get(f.object);
        String containerName = param.conf.getSwiftContainer();
        FileSystemBooleanResult result = new FileSystemBooleanResult();
        result.bool = getExistInner(f, containerName);
        return result;
    }

    private boolean getExistInner(FileObject f, String containerName) {
        boolean exist;
        try {
            DirectoryOrObject mydir = dooMap.get(f.object);
            String dirName = mydir.getName();
            Container container = conf.account.getContainer(containerName);
            StoredObject so = container.getObject(dirName);
            // note that a directory does not exist, only files
            if (so.exists()) {
                exist = true;
            } else {
                Collection<DirectoryOrObject> list = container.listDirectory(dirName, '/', null, 1);
                exist = !list.isEmpty();
            }
        } catch (Exception e) {
            log.error(Constants.EXCEPTION, e);
            exist = false;
        }
        return exist;
    }

    @Override
    public FileSystemPathResult getAbsolutePath(FileSystemFileObjectParam param) {
        FileObject f = param.fo;
        String p = getAbsolutePathInner(f);
        FileSystemPathResult result = new FileSystemPathResult();
        result.setPath(p);
        return result;
    }

    private String getAbsolutePathInner(FileObject f) {
        DirectoryOrObject path = dooMap.get(f.object);
        String p = FileSystemConstants.SWIFT + path.getName();
        return p;
    }

    @Override
    public FileSystemBooleanResult isDirectory(FileSystemFileObjectParam param) {
        FileObject f = param.fo;
        boolean isDirectory = isDirectoryInner(f);
        FileSystemBooleanResult result = new FileSystemBooleanResult();
        result.bool = isDirectory;
        return result;	    
    }

    private boolean isDirectoryInner(FileObject f) {
        boolean isDirectory;
        try {
            DirectoryOrObject doo = dooMap.get(f.object);
            isDirectory = doo.isDirectory();
        } catch (Exception e) {
            log.error(Constants.EXCEPTION, e);
            isDirectory = false;
        }
        return isDirectory;
    }

    @Override
    public FileSystemByteResult getInputStream(FileSystemFileObjectParam param) {
        FileSystemByteResult result = new FileSystemByteResult();
        result.bytes = getInputStreamInner(param.fo);
        return result;
    }

    private byte[] getInputStreamInner(FileObject f) {
        byte[] bytes;
        try {
            DirectoryOrObject doo = dooMap.get(f.object);
            StoredObject so = doo.getAsObject();
            bytes = so.downloadObject();
        } catch (Exception e) {
            log.error(Constants.EXCEPTION, e);
            return null;
        }
        return bytes;
    }

    @Override
    public FileSystemMyFileResult getWithInputStream(FileSystemPathParam param) {
        String containerName = param.conf.getSwiftContainer();
        Map<String, MyFile> map = new HashMap<>();
        for (String filename : param.paths) {
            FileObject[] fo = getInner(filename, containerName);
            MyFile my = getMyFile(containerName, fo, true);
            map.put(filename, my);
        }
        FileSystemMyFileResult result = new FileSystemMyFileResult();
        result.map = map;
        return result;
    }

    private MyFile getMyFile(String containerName, FileObject[] fo, boolean withBytes) {
        MyFile my = new MyFile();
        my.fileObject = fo;
        if (fo[0] != null) {
            my.exists = getExistInner(fo[0], containerName);
            if (my.exists) {
                my.isDirectory = isDirectoryInner(fo[0]);
                my.absolutePath = getAbsolutePathInner(fo[0]);
                if (withBytes) {
                    my.bytes = getInputStreamInner(fo[0]);
                }
            }
        }
        return my;
    }

    @Override
    public FileSystemFileObjectResult getParent(FileSystemFileObjectParam param) {
        FileObject f = param.fo;
        DirectoryOrObject doo = dooMap.get(f.object);
        String name = doo.getName();
        File fi = new File(name);
        String parent = fi.getParent();
        DirectoryOrObject pardoo = new Directory(parent, '/');
        FileSystemFileObjectResult result = new FileSystemFileObjectResult();
        FileObject[] fo = new FileObject[1];
        fo[0] = new FileObject(pardoo.getName(), this.getClass().getSimpleName());
        result.setFileObject(fo);
        dooMap.put(pardoo.getName(), pardoo);
        return result;
    }

    @Override
    public FileSystemFileObjectResult get(FileSystemPathParam param) {
        FileObject[] fos = getInner(param.path, param.conf.getSwiftContainer());
        FileSystemFileObjectResult result = new FileSystemFileObjectResult();
        result.setFileObject(fos);
        return result;
    }

    private FileObject[] getInner(String string, String containerName) {
        FileObject[] fos = new FileObject[1];
        try {
            if (string.startsWith(FileSystemConstants.SWIFT)) {
                string = string.substring(FileSystemConstants.SWIFTLEN);
            }
            // Joss directories don't start with /
            if (string.startsWith("/")) {
                string = string.substring(1);
            }
            Container container = conf.account.getContainer(containerName);
            StoredObject so = container.getObject(string);
            FileObject fo;
            // if it exists, it is a file and not a dir
            if (so.exists()) {
                fo = new FileObject(so.getName(), this.getClass().getSimpleName());
                dooMap.put(string, so);
            } else {
                fo = new FileObject(string, this.getClass().getSimpleName());
                dooMap.put(string, new Directory(string, '/'));
            }
            fos[0] = fo;
        } catch (Exception e) {
            log.error("Exception", e);
            return null;
        }
        return fos;
    }

    @Override
    public FileSystemConstructorResult destroy() {
        return null;
    }

}
