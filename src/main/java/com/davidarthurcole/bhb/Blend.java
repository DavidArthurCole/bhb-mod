package com.davidarthurcole.bhb;

import java.util.*;

public class Blend {

    private Blend(){}

    //Instantiate a new random object
    public static Random random = new Random(new Random().nextInt(Integer.MAX_VALUE));

    private static String padWithZeros(String inputString){
        return String.format("%1$" + 2 + "s", inputString).replace(' ', '0');
    }

    //Boolean is the hex valid; ie, does it contain any invalid chars, is it 6 chars, etc.
    public static boolean isHexOk(String hex){
        return(hex != null && hex.matches("^[a-fA-F0-9]+$") && hex.length() == 6);
    }

    //Generate a random hex string of length 6
    public static String generateRandomHex(){
        //New builder object
        StringBuilder rndHex = new StringBuilder();
        //Get a new random char from the string, 6 times
        for (int i = 0; i < 6; i++) rndHex.append("0123456789ABCDEF".charAt(random.nextInt(16)));
        return rndHex.toString();
    }

    public static List<Integer> findSplitLengths(String word, int numSplits){

        int len = word.length();

        //Store the length each substring should be
        List<Integer> solution = new ArrayList<>();

        int roughDivision = (int) Math.ceil( (double) len/numSplits); // the length of each divided word
        int remainingLetters = word.length();

        boolean reduced = false; // flag to see if I've already reduced the size of the sub-words

        for (int i = 0; i < numSplits; ++i) {


            int x = (roughDivision-1)*(numSplits-(i)); // see next comment
            // checks to see if a reduced word length * remaining splits exactly equals remaining letters
            if (!reduced && x == remainingLetters) {
                roughDivision -= 1;
                reduced = true;
            }

            solution.add(roughDivision);
            remainingLetters -= roughDivision;
        }

        return solution;
    }

    public static List<String> determineSplits(boolean rightJustified, List<Integer> splitLengths, String input){

        List<String> result = new ArrayList<>(splitLengths.size());
        if(rightJustified) Collections.reverse(splitLengths);
        int index = 0;
        for(int i = 0; i < splitLengths.size(); ++i){
            result.add(i, input.substring(index, index+splitLengths.get(i)));
            index+=splitLengths.get(i);
        }

        return result;
    }

    public static String blendTwo(String hexOne, String hexTwo, String input){

        //Output will be appended over time
        StringBuilder output = new StringBuilder();

        //Loop through each step
        for(float j = 0; j <= (input.length() - 1); ++j){
            output.append("&#").append((padWithZeros(Integer.toHexString(Integer.parseInt(hexOne.substring(0, 2), 16) +
                    (int) ((j / (input.length() - 1)) * (Integer.parseInt(hexTwo.substring(0, 2), 16) - Integer.parseInt(hexOne.substring(0, 2), 16)))))
                    + padWithZeros(Integer.toHexString(Integer.parseInt(hexOne.substring(2, 4), 16) +
                    (int) ((j / (input.length() - 1)) * (Integer.parseInt(hexTwo.substring(2, 4), 16) - Integer.parseInt(hexOne.substring(2, 4), 16)))))
                    + padWithZeros(Integer.toHexString(Integer.parseInt(hexOne.substring(4, 6), 16) +
                    (int) ((j / (input.length() - 1)) * (Integer.parseInt(hexTwo.substring(4, 6), 16) - Integer.parseInt(hexOne.substring(4, 6), 16)))))).toUpperCase()).append(input.charAt((int) j));
        }

        return output.toString();
    }

    public static List<String> blendMain(int howManyCodes, String input, List<String> codeArray, boolean rightJustified){

        //New builder
        List<String> returnList = new ArrayList<>();
        int codeIndex = 0;
        List<Integer> splitLengths = findSplitLengths(input, (howManyCodes - 1));

        List<String> splits = determineSplits(rightJustified, splitLengths, input);

        for(int i = 0; i < splits.size(); i++ ){
            if(i != (splits.size() -1)) splits.set(i, splits.get(i) + splits.get(i + 1).charAt(0));

            String addendum = blendTwo(codeArray.get(codeIndex), codeArray.get(codeIndex + 1), splits.get(i));
            returnList.add(i != (splits.size() - 1) ? addendum.substring(0, addendum.length() - 9) : addendum);
            codeIndex++;
        }

        return returnList;
    }
}
