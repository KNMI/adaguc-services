package nl.knmi.adaguc.services.basket;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import lombok.Getter;
import nl.knmi.adaguc.config.ConfigurationItemNotFoundException;
import nl.knmi.adaguc.config.MainServicesConfigurator;

@Getter
public class Basket {
	private String userDir;
	private String name;
	private BasketNode rootNode;

	public Basket(String dir, String name) {
		this.userDir=dir;
		if (!this.userDir.endsWith("/")){
			this.userDir+="/";
		}
		this.name=name;
		this.rootNode=null;
		try {
			this.rootNode=listFiles();
		} catch (ConfigurationItemNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public BasketNode listFiles() throws ConfigurationItemNotFoundException{
		BasketNode rootBn=new BasketNode(this.name, "root", null, null, null);
		return this.listFiles(rootBn, userDir);	
	}
	
	public BasketNode listFiles(BasketNode bn, String dir) throws ConfigurationItemNotFoundException {
		String externalURL=MainServicesConfigurator.getServerExternalURL();
		File d=new File(dir);
		if (d.isDirectory()) {
			String[] filesIndir=d.list();
			for (String fn: filesIndir) {
				String fullPath=dir+fn;
				String cleanPath=fullPath.replace(this.userDir,  "");
				File f=new File(fullPath);
				String id=this.name+"/"+cleanPath;
				String dapUrl=externalURL+"/opendap/"+this.name+"/"+cleanPath;
				String httpUrl=externalURL+"/opendap/"+this.name+"/"+cleanPath;
				if (f.isFile()) {
					BasketNode newBasketNode=new BasketNode(fn,  "leaf", id, dapUrl, httpUrl);
					newBasketNode.setSize(f.length());
					newBasketNode.setDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(f.lastModified()), ZoneId.of("UTC")));
					bn.addChild(newBasketNode);
				} else if (f.isDirectory()) {
					BasketNode dirNode=new BasketNode(fn, "node", id, null, null);
					dirNode.setDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(f.lastModified()), ZoneId.of("UTC")));
					bn.addChild(listFiles(dirNode, dir+fn+"/"));
				}
			}
		}
		return bn;
	}


	public static void main(String[]argv) {
		Basket b=new Basket("/nobackup/users/vreedede/testimpactspace", "testBasket");
		try {
			System.err.println(b.listFiles());
		} catch (ConfigurationItemNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
