package com.wheelly.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BackupUtils {
	public static boolean copyDatabase(String sourcePath, String destinationPath) {
		boolean result = true;
		InputStream input = null;
		OutputStream output = null;
		
		try {
			input = new FileInputStream(sourcePath);
			output = new FileOutputStream(destinationPath);
			
			byte[] buffer = new byte[1024];
			int length;
			try {
				while ((length = input.read(buffer))>0) {
					output.write(buffer, 0, length);
				}
			} catch (IOException e) {
				return false;
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} finally {
			if(null != output) {
				try {
					output.close();
				} catch (IOException e) {
					result = false;
				}
			}
			
			if(null != input) {
				try {
					input.close();
				} catch (IOException e) {
					result = false;
				}
			}
		}
		return result;
	}
}