package com.wheelly.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BackupUtils {
	public static boolean backupDatabase(String sourcePath, String destinationPath) {
		try {
			InputStream input = new FileInputStream(new File(sourcePath));
			OutputStream output = new FileOutputStream(destinationPath);
			
			byte[] buffer = new byte[1024];
			int length;
			try {
				while ((length = input.read(buffer))>0) {
					output.write(buffer, 0, length);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
}