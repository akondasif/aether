package roart.queue;

import java.util.List;

import roart.model.Index;

import org.apache.tika.metadata.Metadata;

public class TikaQueueElement {

    public int size;
    public String dbfilename;
    public String filename;
    public String md5;
    public Index index;
    public List<String> retlist;
    public Metadata metadata;

    public TikaQueueElement(String dbfilename, String filename, String md5, Index index, List<String> retlist, Metadata metadata) {
	this.dbfilename = dbfilename;
	this.filename = filename;
	this.md5 = md5;
	this.index = index;
	this.retlist = retlist;
	this.metadata = metadata;
    }

}