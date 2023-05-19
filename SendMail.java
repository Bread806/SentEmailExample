package com.efgp.sb;

import javax.ejb.Remote;

@Remote
public interface SendMail {
	public void sendMail();
}
