package nl.knmi.adaguc.services.basket;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;

import lombok.Getter;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.ElementNotFoundException;
import nl.knmi.adaguc.config.MainServicesConfigurator;
import nl.knmi.adaguc.security.user.User;

@Getter
public class Basket {
	private String userDir;
	private String name;
	private BasketNode rootNode;
	private String token;

	public Basket(String dir, String name, String token) {
		Debug.println("New basket for " + name);
		this.userDir=dir;
		this.token=token;
		if (!this.userDir.endsWith("/")){
			this.userDir+="/";
		}
		this.name=name;
		this.rootNode=null;
		try {
			this.rootNode=listFiles();
		} catch (ElementNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private BasketNode listFiles() throws ElementNotFoundException{
		BasketNode rootBn=new BasketNode(this.name, "root", null, null, null);
		return this.listFiles(rootBn, userDir);	
	}
	
	public static String GetRemotePrefix(User user) throws ElementNotFoundException {
		return MainServicesConfigurator.getServerExternalURL() + "/opendap/"+User.makePosixUserId(user.getUserId()) + "/";
	}
	
	
	
	private BasketNode listFiles(BasketNode bn, String dir) throws ElementNotFoundException {
		String externalURL=MainServicesConfigurator.getServerExternalURL();
		File d=new File(dir);
		if (d.isDirectory()) {
			String[] filesIndir=d.list();
			Arrays.sort(filesIndir);
			for (String fn: filesIndir) {
				String fullPath=dir+fn;
				String cleanPath=fullPath.replace(this.userDir,  "");
				File f=new File(fullPath);
				String id=this.name+"/"+cleanPath;
				String tokenPart="";
				if (token!=null) {
					tokenPart=token+"/";
				}
				String dapUrl=externalURL+"/opendap/"+tokenPart+this.name+"/"+cleanPath;
				String httpUrl=externalURL+"/opendap/"+tokenPart+this.name+"/"+cleanPath;
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


	
}
