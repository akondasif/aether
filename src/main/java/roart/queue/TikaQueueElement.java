package roart.queue;

import java.util.List;

import roart.model.IndexFiles;
import roart.model.ResultItem;;

import org.apache.tika.metadata.Metadata;

public class TikaQueueElement {

    public int size;
    public String dbfilename;
    public String filename;
    public String md5;
    public IndexFiles index;
    public List<ResultItem> retlist;
    public List<ResultItem> retlistnot;
    public Metadata metadata;
    public String convertsw;

    public TikaQueueElement(String dbfilename, String filename, String md5, IndexFiles index, List<ResultItem> retlist, List<ResultItem> retlistnot, Metadata metadata) {
	this.dbfilename = dbfilename;
	this.filename = filename;
	this.md5 = md5;
	this.index = index;
	this.retlist = retlist;
	this.retlistnot = retlistnot;
	this.metadata = metadata;
    }

}
