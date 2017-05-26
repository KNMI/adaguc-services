package nl.knmi.adaguc.security.token;

import java.text.ParseException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.Setter;
import nl.knmi.adaguc.security.user.User;
import nl.knmi.adaguc.tools.DateFunctions;


@JsonInclude(Include.NON_NULL)
@Getter
@Setter
public class Token {
	Token(){
		
	}
	
	String token; 
	String userId;
	String currentDate;
	String notBefore;
	String notAfter;
	int defaultValidityHours = 24;
	public Token(String id, User user) throws ParseException  {
		this.userId = user.getUserId();
		this.token = id;
	      currentDate = DateFunctions.getCurrentDateInISO8601();
	      notBefore = currentDate;
	      notAfter = DateFunctions.dateAddStepInStringFormat(currentDate, "hour",defaultValidityHours);
	}

}
