package com.luyun.easyway95;

import android.util.Log;

public class LYCheckSum {
	private static final String TAG = "LYCheckSum";

	static byte[] genCheckSum(byte[] payload) {
		byte[] checkSum = {0, 0, 0, 0};
		if (payload == null || payload.length == 0) {
			return checkSum;
		}
		checkSum = Constants.SHARED_SECRET.getBytes();
		int howManyWords = payload.length/4;
		int remainder = payload.length % 4;
		
		for (int i=0; i<howManyWords; i++) {
			for (int j=0; j<4; j++) {
				checkSum[j] = (byte)( (byte)(checkSum[j]) ^ (byte) (payload[i*4+j]));
			}
		}
		if (remainder != 0) {
			int j;
			for (j=0; j<remainder; j++) {
				checkSum[j] = (byte)( (byte)(checkSum[j]) ^ (byte) (payload[howManyWords*4+j]));
			}
			for (; j<4; j++) {
				checkSum[j] = (byte)( (byte)(checkSum[j]) ^ (byte) (0));
			}
		}
		return checkSum;
		//Log.d(TAG, new String(checkSum));
		//修改了返回值，原来返回一个整形现在返回4个字节
		
//		int value = 0;
//		int mask = 0xff;
//		for (int j=0; j<4; j++) {
//			value <<= 8;
//			int tmpValue = checkSum[j] & mask;
//			value = value | tmpValue;
//		}
//		return value;
	}
}
