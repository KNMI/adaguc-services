package nl.knmi.adaguc.services.basket;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BasketNode {
	private enum NodeType {ROOT,LEAF, NODE};

	private NodeType type;
	private String name;
	private String id;
	private String dapurl;
	private String httpurl;
	private List<BasketNode> children;
	private Long size;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
	private LocalDateTime date;

	
	public BasketNode(String name, String type, String id, String dapUrl, String httpUrl) {
		this.name=name;
		this.size=null;
		if (type.equals("node")) {
			this.type=NodeType.NODE;
			this.children=new ArrayList<BasketNode>();
		} else if (type.equals("leaf")){
			this.type=NodeType.LEAF;
		} else {
			this.type=NodeType.ROOT;
			this.children=new ArrayList<BasketNode>();
		}
		this.id=id;
		this.httpurl=httpUrl;
		this.dapurl=dapUrl;
	}
	
	public void addChildren(List<BasketNode>nodes) {
		children.addAll(nodes);
	}

	public void addChild(BasketNode bn) {
		children.add(bn);
	}

	public String toString() {
		StringBuffer sb=new StringBuffer("");
		sb.append("name: "+name+", ");
		sb.append("typex:"+type+", ");
		if (type==NodeType.NODE) {
			sb.append("children: [");
			for (BasketNode bn: children) {
				sb.append(bn.toString()+",");
			}
			sb.append("]");
		}
		sb.append("\n");
        return sb.toString();
	}
	

}

