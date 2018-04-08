package com.silenove.huffman;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		File file = new File("test.txt");
		try {
			FileOutputStream file_out = new FileOutputStream(file);
			file_out.write(190066);
			file_out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			byte tmp;
			FileInputStream file_in = new FileInputStream(file);
			while((tmp = (byte)file_in.read()) != -1) {
				System.out.println(tmp);
			}
			file_in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
