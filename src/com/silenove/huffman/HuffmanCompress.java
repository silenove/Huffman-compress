package com.silenove.huffman;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

public class HuffmanCompress {

	//Huffman压缩
	public void compress(String fname_in, String fname_out) {
		
		File inputFile = new File(fname_in);
		File outputFile = new File(fname_out);
		// 保存不同字符的出现次数
		CmpNode[] cmpNodes = new CmpNode[256];
		int length = 0; // 记录文档长度
		int byte_tmp, byte_catg = 0;
		for (int i = 0; i < 256; i++) {
			cmpNodes[i] = new CmpNode((byte) i, 0);
		}
		try {
			FileInputStream file_in = new FileInputStream(inputFile);
			// 记录不同字符出现次数
			while ((byte_tmp = file_in.read()) != -1) {
				cmpNodes[byte_tmp].weight++;
				length++;
			}
			file_in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// 字符案出现次数排序
		Arrays.sort(cmpNodes);
		// 记录文档中出现的字符种类个数
		for (int i = 0; i < 256; i++) {
			if (cmpNodes[i].weight == 0) {
				byte_catg = i;
				break;
			}
		}
		// huffman树节点个数
		int node_sum = byte_catg * 2 - 1;

		HTreeNode[] htree = new HTreeNode[node_sum];
		for (int i = 0; i < byte_catg; i++) {
			htree[i] = new HTreeNode(cmpNodes[i].ch, cmpNodes[i].weight, -1, -1, -1, i);
			htree[i].huffman = "";
		}
		for (int i = byte_catg; i < node_sum; i++) {
			htree[i] = new HTreeNode();
			htree[i].huffman = "";
			htree[i].index = i;
			htree[i].parent = -1;
			htree[i].lchild = -1;
			htree[i].rchild = -1;
		}
		
		//构建Huffman树
		this.createHTree(htree, byte_catg, node_sum);
		//构建Huffman编码
		this.createHCode(htree, node_sum-1);
		
		//构建hashmap，字节->Huffman编码
		HashMap<Byte, String> map = new HashMap<>();
		for(int i=0;i < byte_catg; i++) {
			map.put(htree[i].ch, htree[i].huffman);
		}
		
		try {
			FileInputStream file_in = new FileInputStream(inputFile);
			FileOutputStream file_out = new FileOutputStream(outputFile);
			
			//写入字符种类数目
			file_out.write(byte_catg);
			//写入字符及其Huffman编码
			for(int i=0; i < byte_catg; i++) {
				String code = map.get(htree[i].ch) + "\r\n";
				file_out.write(htree[i].ch);
				file_out.write(code.getBytes("UTF-8"));
			}
			
			//写入文档长度
			byte[] byte_length = new byte[4];
			byte_length[0] = (byte)((length >> 24) & 0xff);
			byte_length[1] = (byte)((length >> 16) & 0xff);
			byte_length[2] = (byte)((length >> 8) & 0xff);
			byte_length[3] = (byte)(length & 0xff);
			file_out.write(byte_length);
			
			//将Huffman编码输出二进制文件
			String buffer = "";
			while((byte_tmp = file_in.read()) != -1) {
				buffer += map.get((byte)byte_tmp);
				
				while(buffer.length() >= 8) {
					int tmp = 0;
					for(int i=0;i < 8; i++) {
						tmp <<= 1;
						if(buffer.charAt(i) == '1') {
							tmp |= 1;
						}
					}
					file_out.write((byte)tmp);
					buffer = buffer.substring(8);
				}
			}
			
			if(buffer.length() > 0) {
				for(int i=buffer.length(); i < 8; i++) {
					buffer += "0";
				}
				int tmp = 0;
				for(int i=0;i < 8; i++) {
					tmp <<= 1;
					if(buffer.charAt(i) == '1') {
						tmp |= 1;
					}
				}
				file_out.write((byte)tmp);	
			}
			
			file_in.close();
			file_out.close();
			
			System.out.println("compress finished!");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	//Huffman解压缩
	public void decompress(String fname_in, String fname_out) {
		
		File inputFile = new File(fname_in);
		File outputFile = new File(fname_out);
		HashMap<String, Byte> map = new HashMap<>();
		int byte_catg, length=0;
		byte tmp;
		try {
			FileInputStream file_in = new FileInputStream(inputFile);
			FileOutputStream file_out = new FileOutputStream(outputFile);
			
			//读取Huffman编码
			byte_catg = file_in.read();
			for(int i=0; i < byte_catg; i++) {
				String code = "";
				byte ch = (byte)file_in.read();
				while((tmp = (byte)file_in.read()) != '\r') {
					code += (char)tmp;
				}
				
				map.put(code, ch);
				file_in.read();
			}
			
			//读入文档字节长度
			byte[] byte_length = new byte[4];
			for(int i=0; i < 4; i++) {
				byte_length[i] = (byte)file_in.read();
			}
			for(int i=0; i < 4; i++) {
				length += (byte_length[i] & 0x000000ff) << ((4-1-i)*8);
			}
			
					
			//Huffman解码
			String code = "";
			while(length > 0) {
				tmp = (byte)file_in.read();
				for(int i=0; i < 8; i++) {
					if((tmp & 0x80) == 0x80) {
						code += '1';
					}else {
						code += '0';
					}
					
					if(map.containsKey(code)) {
						file_out.write(map.get(code));
						code="";
						length--;
						
					}		
					tmp <<= 1;
				}
				
			}
			
			file_in.close();
			file_out.close();
			
			System.out.println("decompress finished!");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	// 建立Huffman树
	public void createHTree(HTreeNode[] htree, int byte_catg, int node_sum) {
		PriorityQueue<HTreeNode> queue = new PriorityQueue<>(10, new Compare());
		HTreeNode node1 = null, node2 = null;
		for (int i = 0; i < byte_catg; i++) {
			queue.add(htree[i]);
		}

		for (int i = byte_catg; i < node_sum; i++) {
			node1 = queue.poll();
			node2 = queue.poll();
			htree[node1.index].parent = i;
			htree[node2.index].parent = i;
			htree[i].lchild = node1.index;
			htree[i].rchild = node2.index;
			htree[i].weight = node1.weight + node2.weight;

			queue.add(htree[i]);
		}

	}

	//构建Huffman编码
	public void createHCode(HTreeNode[] htree, int index) {
		if(htree[index].lchild == -1) {
			return;
		}
		htree[htree[index].lchild].huffman = htree[index].huffman + '0';
		htree[htree[index].rchild].huffman += htree[index].huffman + '1';
		createHCode(htree, htree[index].lchild);
		createHCode(htree, htree[index].rchild);
		return;
	}

	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		HuffmanCompress compress = new HuffmanCompress();
		
		//huffman编码压缩Aesop_Fables.txt并解压
		compress.compress("Aesop_Fables.txt", "Aesop_Fables_compress.txt");
		compress.decompress("Aesop_Fables_compress.txt", "Aesop_Fables_decompress.txt");
		
		//huffman编码压缩graph.txt并解压
		compress.compress("graph.txt", "graph_compress.txt");
		compress.decompress("graph_compress.txt", "graph_decompress.txt");
		
	}

}

//huffman树节点
class HTreeNode {
	public byte ch;
	public int weight, index;
	public String huffman;
	public int parent, lchild, rchild;

	public HTreeNode(byte ch, int weight, int parent, int lchild, int rchild, int index) {
		super();
		this.ch = ch;
		this.weight = weight;
		this.parent = parent;
		this.lchild = lchild;
		this.rchild = rchild;
		this.index = index;
		this.huffman = "";
	}

	public HTreeNode() {
	}

}

//临时节点，用于记录字符出现次数
class CmpNode implements Comparable<CmpNode> {

	public byte ch;
	public int weight;

	public CmpNode(byte ch, int weight) {
		super();
		this.ch = ch;
		this.weight = weight;
	}

	@Override
	public int compareTo(CmpNode o) {
		// TODO Auto-generated method stub
		if (this.weight < o.weight) {
			return 1;
		} else if (this.weight > o.weight) {
			return -1;
		}
		return 0;
	}

}

//用于优先队列排序
class Compare implements Comparator<HTreeNode> {

	@Override
	public int compare(HTreeNode o1, HTreeNode o2) {
		// TODO Auto-generated method stub
		if (o1.weight < o2.weight) {
			return -1;
		} else if (o1.weight > o2.weight) {
			return 1;
		}
		return 0;
	}

}
