/*
 * Author: Jonathan Link
 * Email: jonathanlink[d o t]email[a t]gmail[d o t]com
 * Date of creation: 13.11.2014
 * Version: 2.2.3
 * Description:
 *
 * Version 2.1 uses PDFBox 2.x. Version 1.0 used PDFBox 1.8.x
 * Acknowledgement to James Sullivan for version 2.0
 *
 * What does it DO:
 * This object converts the content of a PDF file into a String.
 * The layout of the texts is transcribed as near as the one in the PDF given at the input.
 *
 * What does it NOT DO:
 * Vertical texts in the PDF file are not handled for the moment.
 *
 * I would appreciate any feedback you could offer. (see my email address above)
 *
 * LICENSE:
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2019 Jonathan Link
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package com.wxdi.pdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.text.TextPositionComparator;

public class PDFLayoutTextStripper extends PDFTextStripper {

    public static final boolean DEBUG = false;
    public static final int OUTPUT_SPACE_CHARACTER_WIDTH_IN_PT = 4;

    private double currentPageWidth;
    private TextPosition previousTextPosition;
    private List<TextLine> textLineList;

    public PDFLayoutTextStripper() throws IOException {
        super();
        this.previousTextPosition = null;
        this.textLineList = new ArrayList<TextLine>();
    }

    @Override
    public void processPage(PDPage page) throws IOException {
        PDRectangle pageRectangle = page.getMediaBox();
        if (pageRectangle!= null) {
            this.setCurrentPageWidth(pageRectangle.getWidth());
            super.processPage(page);
            this.previousTextPosition = null;
            this.textLineList = new ArrayList<TextLine>();
        }
    }

    @Override
    protected void writePage() throws IOException {
        List<List<TextPosition>> charactersByArticle = super.getCharactersByArticle();
        for( int i = 0; i < charactersByArticle.size(); i++) {
            List<TextPosition> textList = charactersByArticle.get(i);
            try {
                this.sortTextPositionList(textList);
            } catch ( java.lang.IllegalArgumentException e) {
                System.err.println(e);
            }
            this.iterateThroughTextList(textList.iterator()) ;
        }
        this.writeToOutputStream(this.getTextLineList());
    }

    private void writeToOutputStream(final List<TextLine> textLineList) throws IOException {
        for (TextLine textLine : textLineList) {
            char[] line = textLine.getLine().toCharArray();
            super.getOutput().write(line);
            super.getOutput().write('\n');
            super.getOutput().flush();
        }
    }

    /*
     * In order to get rid of the warning:
     * TextPositionComparator class should implement Comparator<TextPosition> instead of Comparator
     */
    @SuppressWarnings("unchecked")
    private void sortTextPositionList(final List<TextPosition> textList) {
        TextPositionComparator comparator = new TextPositionComparator();
        Collections.sort(textList, comparator);
    }

    private void writeLine(final List<TextPosition> textPositionList) {
        if ( textPositionList.size() > 0 ) {
            TextLine textLine = this.addNewLine();
            boolean firstCharacterOfLineFound = false;
            for (TextPosition textPosition : textPositionList ) {
                CharacterFactory characterFactory = new CharacterFactory(firstCharacterOfLineFound);
                Character character = characterFactory.createCharacterFromTextPosition(textPosition, this.getPreviousTextPosition());
                textLine.writeCharacterAtIndex(character);
                this.setPreviousTextPosition(textPosition);
                firstCharacterOfLineFound = true;
            }
        } else {
            this.addNewLine(); // white line
        }
    }

    private void iterateThroughTextList(Iterator<TextPosition> textIterator) {
        List<TextPosition> textPositionList = new ArrayList<TextPosition>();

        while ( textIterator.hasNext() ) {
            TextPosition textPosition = (TextPosition)textIterator.next();
            int numberOfNewLines = this.getNumberOfNewLinesFromPreviousTextPosition(textPosition);
            if ( numberOfNewLines == 0 ) {
                textPositionList.add(textPosition);
            } else {
                this.writeTextPositionList(textPositionList);
                this.createNewEmptyNewLines(numberOfNewLines);
                textPositionList.add(textPosition);
            }
            this.setPreviousTextPosition(textPosition);
        }
        if (!textPositionList.isEmpty()) {
            this.writeTextPositionList(textPositionList);
        }
    }

    private void writeTextPositionList(final List<TextPosition> textPositionList) {
        this.writeLine(textPositionList);
        textPositionList.clear();
    }

    private void createNewEmptyNewLines(int numberOfNewLines) {
        for (int i = 0; i < numberOfNewLines - 1; ++i) {
            this.addNewLine();
        }
    }

    private int getNumberOfNewLinesFromPreviousTextPosition(final TextPosition textPosition) {
        TextPosition previousTextPosition = this.getPreviousTextPosition();
        if ( previousTextPosition == null ) {
            return 1;
        }

        float textYPosition = Math.round( textPosition.getY() );
        float previousTextYPosition = Math.round( previousTextPosition.getY() );

        if ( textYPosition > previousTextYPosition && (textYPosition - previousTextYPosition > 5.5) ) {
            double height = textPosition.getHeight();
            int numberOfLines = (int) (Math.floor( textYPosition - previousTextYPosition) / height );
            numberOfLines = Math.max(1, numberOfLines - 1); // exclude current new line
            if (DEBUG) System.out.println(height + " " + numberOfLines);
            return numberOfLines ;
        } else {
            return 0;
        }
    }

    private TextLine addNewLine() {
        TextLine textLine = new TextLine(this.getCurrentPageWidth());
        textLineList.add(textLine);
        return textLine;
    }

    private TextPosition getPreviousTextPosition() {
        return this.previousTextPosition;
    }

    private void setPreviousTextPosition(final TextPosition setPreviousTextPosition) {
        this.previousTextPosition = setPreviousTextPosition;
    }

    private int getCurrentPageWidth() {
        return (int) Math.round(this.currentPageWidth);
    }

    private void setCurrentPageWidth(double currentPageWidth) {
        this.currentPageWidth = currentPageWidth;
    }

    private List<TextLine> getTextLineList() {
        return this.textLineList;
    }

}

class TextLine {

    private static final char SPACE_CHARACTER = ' ';
    private int lineLength;
    private String line;
    private int lastIndex;

    public TextLine(int lineLength) {
        this.line = "";
        this.lineLength = lineLength / PDFLayoutTextStripper.OUTPUT_SPACE_CHARACTER_WIDTH_IN_PT;
        this.completeLineWithSpaces();
    }

    public void writeCharacterAtIndex(final Character character) {
        character.setIndex(this.computeIndexForCharacter(character));
        int index = character.getIndex();
        char characterValue = character.getCharacterValue();
        if ( this.indexIsInBounds(index) && this.line.charAt(index) == SPACE_CHARACTER) {
            this.line = this.line.substring(0, index) + characterValue + this.line.substring(index + 1, this.getLineLength());
        }
    }

    public int getLineLength() {
        return this.lineLength;
    }

    public String getLine() {
        return line;
    }

    private int computeIndexForCharacter(final Character character) {
        int index = character.getIndex();
        boolean isCharacterPartOfPreviousWord = character.isCharacterPartOfPreviousWord();
        boolean isCharacterAtTheBeginningOfNewLine = character.isCharacterAtTheBeginningOfNewLine();
        boolean isCharacterCloseToPreviousWord = character.isCharacterCloseToPreviousWord();

        if ( !this.indexIsInBounds(index) ) {
            return -1;
        } else {
            if ( isCharacterPartOfPreviousWord && !isCharacterAtTheBeginningOfNewLine ) {
                index = this.findMinimumIndexWithSpaceCharacterFromIndex(index);
            } else if ( isCharacterCloseToPreviousWord ) {
                if ( this.line.charAt(index) != SPACE_CHARACTER ) {
                    index = index + 1;
                } else {
                    index = this.findMinimumIndexWithSpaceCharacterFromIndex(index) + 1;
                }
            }
            index = this.getNextValidIndex(index, isCharacterPartOfPreviousWord);
            return index;
        }
    }

    private boolean isSpaceCharacterAtIndex(int index) {
        return this.line.charAt(index) != SPACE_CHARACTER;
    }

    private boolean isNewIndexGreaterThanLastIndex(int index) {
        int lastIndex = this.getLastIndex();
        return ( index > lastIndex );
    }

    private int getNextValidIndex(int index, boolean isCharacterPartOfPreviousWord) {
        int nextValidIndex = index;
        int lastIndex = this.getLastIndex();
        if ( ! this.isNewIndexGreaterThanLastIndex(index) ) {
            nextValidIndex = lastIndex + 1;
        }
        if ( !isCharacterPartOfPreviousWord && this.isSpaceCharacterAtIndex(index - 1) ) {
            nextValidIndex = nextValidIndex + 1;
        }
        this.setLastIndex(nextValidIndex);
        return nextValidIndex;
    }

    private int findMinimumIndexWithSpaceCharacterFromIndex(int index) {
        int newIndex = index;
        while( newIndex >= 0 && this.line.charAt(newIndex) == SPACE_CHARACTER ) {
            newIndex = newIndex - 1;
        }
        return newIndex + 1;
    }

    private boolean indexIsInBounds(int index) {
        return (index >= 0 && index < this.lineLength);
    }

    private void completeLineWithSpaces() {
        for (int i = 0; i < this.getLineLength(); ++i) {
            line += SPACE_CHARACTER;
        }
    }

    private int getLastIndex() {
        return this.lastIndex;
    }

    private void setLastIndex(int lastIndex) {
        this.lastIndex = lastIndex;
    }

}


class Character {

    private char characterValue;
    private int index;
    private boolean isCharacterPartOfPreviousWord;
    private boolean isFirstCharacterOfAWord;
    private boolean isCharacterAtTheBeginningOfNewLine;
    private boolean isCharacterCloseToPreviousWord;

    public Character(char characterValue, int index, boolean isCharacterPartOfPreviousWord, boolean isFirstCharacterOfAWord, boolean isCharacterAtTheBeginningOfNewLine, boolean isCharacterPartOfASentence) {
        this.characterValue = characterValue;
        this.index = index;
        this.isCharacterPartOfPreviousWord = isCharacterPartOfPreviousWord;
        this.isFirstCharacterOfAWord = isFirstCharacterOfAWord;
        this.isCharacterAtTheBeginningOfNewLine = isCharacterAtTheBeginningOfNewLine;
        this.isCharacterCloseToPreviousWord = isCharacterPartOfASentence;
        if (PDFLayoutTextStripper.DEBUG) System.out.println(this.toString());
    }

    public char getCharacterValue() {
        return this.characterValue;
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isCharacterPartOfPreviousWord() {
        return this.isCharacterPartOfPreviousWord;
    }

    public boolean isFirstCharacterOfAWord() {
        return this.isFirstCharacterOfAWord;
    }

    public boolean isCharacterAtTheBeginningOfNewLine() {
        return this.isCharacterAtTheBeginningOfNewLine;
    }

    public boolean isCharacterCloseToPreviousWord() {
        return this.isCharacterCloseToPreviousWord;
    }

    public String toString() {
        String toString = "";
        toString += index;
        toString += " ";
        toString += characterValue;
        toString += " isCharacterPartOfPreviousWord=" + isCharacterPartOfPreviousWord;
        toString += " isFirstCharacterOfAWord=" + isFirstCharacterOfAWord;
        toString += " isCharacterAtTheBeginningOfNewLine=" + isCharacterAtTheBeginningOfNewLine;
        toString += " isCharacterPartOfASentence=" + isCharacterCloseToPreviousWord;
        toString += " isCharacterCloseToPreviousWord=" + isCharacterCloseToPreviousWord;
        return toString;
    }

}


class CharacterFactory {

    private TextPosition previousTextPosition;
    private boolean firstCharacterOfLineFound;
    private boolean isCharacterPartOfPreviousWord;
    private boolean isFirstCharacterOfAWord;
    private boolean isCharacterAtTheBeginningOfNewLine;
    private boolean isCharacterCloseToPreviousWord;

    public CharacterFactory(boolean firstCharacterOfLineFound) {
        this.firstCharacterOfLineFound = firstCharacterOfLineFound;
    }

    public Character createCharacterFromTextPosition(final TextPosition textPosition, final TextPosition previousTextPosition) {
        this.setPreviousTextPosition(previousTextPosition);
        this.isCharacterPartOfPreviousWord = this.isCharacterPartOfPreviousWord(textPosition);
        this.isFirstCharacterOfAWord = this.isFirstCharacterOfAWord(textPosition);
        this.isCharacterAtTheBeginningOfNewLine = this.isCharacterAtTheBeginningOfNewLine(textPosition);
        this.isCharacterCloseToPreviousWord = this.isCharacterCloseToPreviousWord(textPosition);
        char character = this.getCharacterFromTextPosition(textPosition);
        int index = (int)textPosition.getX() / PDFLayoutTextStripper.OUTPUT_SPACE_CHARACTER_WIDTH_IN_PT;
        return new Character(character,
                             index,
                             isCharacterPartOfPreviousWord,
                             isFirstCharacterOfAWord,
                             isCharacterAtTheBeginningOfNewLine,
                             isCharacterCloseToPreviousWord);
    }

    private boolean isCharacterAtTheBeginningOfNewLine(final TextPosition textPosition) {
        if ( ! firstCharacterOfLineFound ) {
            return true;
        }
        TextPosition previousTextPosition = this.getPreviousTextPosition();
        float previousTextYPosition = previousTextPosition.getY();
        return ( Math.round( textPosition.getY() ) < Math.round(previousTextYPosition) );
    }

    private boolean isFirstCharacterOfAWord(final TextPosition textPosition) {
        if ( ! firstCharacterOfLineFound ) {
            return true;
        }
        double numberOfSpaces = this.numberOfSpacesBetweenTwoCharacters(previousTextPosition, textPosition);
        return (numberOfSpaces > 1) || this.isCharacterAtTheBeginningOfNewLine(textPosition);
    }

    private boolean isCharacterCloseToPreviousWord(final TextPosition textPosition) {
        if ( ! firstCharacterOfLineFound ) {
            return false;
        }
        double numberOfSpaces = this.numberOfSpacesBetweenTwoCharacters(previousTextPosition, textPosition);
        return (numberOfSpaces > 1 && numberOfSpaces <= PDFLayoutTextStripper.OUTPUT_SPACE_CHARACTER_WIDTH_IN_PT);
    }

    private boolean isCharacterPartOfPreviousWord(final TextPosition textPosition) {
        TextPosition previousTextPosition = this.getPreviousTextPosition();
        if ( previousTextPosition.getUnicode().equals(" ") ) {
            return false;
        }
        double numberOfSpaces = this.numberOfSpacesBetweenTwoCharacters(previousTextPosition, textPosition);
        return (numberOfSpaces <= 1);
    }

    private double numberOfSpacesBetweenTwoCharacters(final TextPosition textPosition1, final TextPosition textPosition2) {
        double previousTextXPosition = textPosition1.getX();
        double previousTextWidth = textPosition1.getWidth();
        double previousTextEndXPosition = (previousTextXPosition + previousTextWidth);
        double numberOfSpaces = Math.abs(Math.round(textPosition2.getX() - previousTextEndXPosition));
        return numberOfSpaces;
    }



    private char getCharacterFromTextPosition(final TextPosition textPosition) {
        String string = textPosition.getUnicode();
        char character = string.charAt(0);
        return character;
    }

    private TextPosition getPreviousTextPosition() {
        return this.previousTextPosition;
    }

    private void setPreviousTextPosition(final TextPosition previousTextPosition) {
        this.previousTextPosition = previousTextPosition;
    }

}
