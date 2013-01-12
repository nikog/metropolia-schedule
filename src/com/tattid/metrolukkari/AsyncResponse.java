package com.tattid.metrolukkari;

import java.io.InputStream;

public class AsyncResponse {
	private CharSequence response;
	private CharSequence responseError;
	
	private InputStream responseStream;
	
	private boolean error;
	
	public AsyncResponse(CharSequence responseString, boolean error) {
		if(error) {
			this.responseError = responseString;
		} else {
			this.response = responseString;
		}
		this.error = error;
	}
	
	public AsyncResponse(InputStream responseStream, boolean error) {
		if(error) {
			this.responseError = "Some sort of error?";
		} else {
			this.responseStream = responseStream;
		}
	}
	public boolean getErr() {
		return error;
	}
	
	public InputStream getResponseStream() {
		return responseStream;
	}
	
	public CharSequence getResponse() {
		return response;
	}
	
	public CharSequence getResponseError() {
		return responseError;
	}
}
