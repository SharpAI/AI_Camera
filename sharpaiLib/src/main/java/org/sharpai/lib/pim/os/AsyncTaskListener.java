package org.sharpai.lib.pim.os;

public interface AsyncTaskListener<T,S> {
	public void onProgressUpdate(T... values);
	public void onPostExecute(S... result);
}
