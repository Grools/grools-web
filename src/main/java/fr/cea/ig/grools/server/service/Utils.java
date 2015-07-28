package fr.cea.ig.grools.server.service;


public class Utils {
    public static String join(String separator, String ... values) {
        if (values.length==0)return "";//need at least one element
        //all string operations use a new array, so minimize all calls possible
        char[] sep = separator.toCharArray();

        // determine final size and normalize nulls
        int totalSize = (values.length - 1) * sep.length;// separator size
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null)
                values[i] = "";
            else
                totalSize += values[i].length();
        }

        //exact size; no bounds checks or resizes
        char[] joined = new char[totalSize];
        int pos = 0;
        //note, we are iterating all the elements except the last one
        for (int i = 0, end = values.length-1; i < end; i++) {
            System.arraycopy(values[i].toCharArray(), 0,
                    joined, pos, values[i].length());
            pos += values[i].length();
            System.arraycopy(sep, 0, joined, pos, sep.length);
            pos += sep.length;
        }
        //now, add the last element;
        //this is why we checked values.length == 0 off the hop
        System.arraycopy(values[values.length-1].toCharArray(), 0,
                joined, pos, values[values.length-1].length());

        return new String(joined);
    }

    public static int[] merge(int[] a, int[] b){
        int length = a.length + b.length;
        int[] result = new int[length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

}
