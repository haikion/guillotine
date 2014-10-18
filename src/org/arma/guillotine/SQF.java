package org.arma.guillotine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SQF {
	private String code;
	
	public void save(File file) throws IOException {
		file.createNewFile();
		FileWriter fw = new FileWriter(file);
		fw.write(code);
		fw.flush();
		fw.close();
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
	
	public String toString() {
		return getCode();
	}
}
