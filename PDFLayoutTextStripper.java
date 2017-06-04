/*
 * Author: Jonathan Link
 * Email: jonathanlink[d o t]email[a t]gmail[d o t]com
 * Date of creation: 13.11.2014
 * Version: 0.1
 * Description:
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
 * Copyright (c) 2014-2015 Jonathan Link
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

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.text.TextPositionComparator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PDFLayoutTextStripper extends PDFTextStripper {

    static final boolean DEBUG = false;
    static final int OUTPUT_SPACE_CHARACTER_WIDTH_IN_PT = 4;

    private double currentPageWidth;
    private TextPosition previousTextPosition;
    private List<TextLine> textLineList;

    PDFLayoutTextStripper() throws IOException {
        super();
        this.previousTextPosition = null;
        this.textLineList = new ArrayList<>();
    }

    @Override
    public void processPage(PDPage page) throws IOException {
        final PDRectangle pageRectangle = page.getMediaBox();
        if (pageRectangle!= null) {
            this.setCurrentPageWidth(pageRectangle.getWidth());
            super.processPage(page);
            this.previousTextPosition = null;
            this.textLineList = new ArrayList<>();
        }
    }

    @Override
    protected void writePage() throws IOException {
        final List <List<TextPosition>> charactersByArticle = super.getCharactersByArticle();
        for (final List<TextPosition> textList : charactersByArticle) {
            this.sortTextPositionList(textList);
            this.iterateThroughTextList(textList.iterator());
        }
        this.writeToOutputStream(this.getTextLineList());
    }

    private void writeToOutputStream(final List<TextLine> textLineList) throws IOException {
        for (final TextLine textLine : textLineList) {
            final char[] line = textLine.getLine().toCharArray();
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
        final TextPositionComparator comparator = new TextPositionComparator();
        textList.sort(comparator);
    }

    private int computeAverageCharacterWidth(final List<TextPosition> textPositionList) {
        if (textPositionList.size() == 0) {
            return 0;
        } else {
            double averageWidth = 0.0;
            for (final TextPosition textPosition : textPositionList) {
                averageWidth += textPosition.getWidthOfSpace();
            }
            return (int) Math.floor(averageWidth) / textPositionList.size();
        }
    }

    private void writeLine(final List<TextPosition> textPositionList) {
        if (textPositionList.size() > 0) {
            final TextLine textLine = this.addNewLine();
            boolean firstCharacterOfLineFound = false;
            for (final TextPosition textPosition : textPositionList ) {
                final CharacterFactory characterFactory = new CharacterFactory(firstCharacterOfLineFound);
                final Character character = characterFactory.createCharacterFromTextPosition(textPosition, this.getPreviousTextPosition());
                textLine.writeCharacterAtIndex(character);
                this.setPreviousTextPosition(textPosition);
                firstCharacterOfLineFound = true;
            }
        } else {
            this.addNewLine(); // white line
        }
    }

    private void iterateThroughTextList(final Iterator<TextPosition> textIterator) {
        final List<TextPosition> textPositionList = new ArrayList<>();

        while (textIterator.hasNext()) {
            final TextPosition textPosition = textIterator.next();
            final int numberOfNewLines = this.getNumberOfNewLinesFromPreviousTextPosition(textPosition);
            if (numberOfNewLines == 0) {
                textPositionList.add(textPosition);
            } else {
                this.writeTextPositionList(textPositionList);
                this.createNewEmptyNewLines(numberOfNewLines);
                textPositionList.add(textPosition);
            }
            this.setPreviousTextPosition(textPosition);
        }
    }

    private void writeTextPositionList(final List<TextPosition> textPositionList) {
        this.writeLine(textPositionList);
        textPositionList.clear();
    }

    private void createNewEmptyNewLines(final int numberOfNewLines) {
        for (int i = 0; i < numberOfNewLines - 1; ++i) {
            this.addNewLine();
        }
    }

    private int getNumberOfNewLinesFromPreviousTextPosition(final TextPosition textPosition) {
        final TextPosition previousTextPosition = this.getPreviousTextPosition();
        if (previousTextPosition == null) {
            return 1;
        }

        final double textYPosition = Math.round(textPosition.getY());
        final double previousTextYPosition = Math.round(previousTextPosition.getY());

        if (textYPosition < previousTextYPosition) {
            final double height = textPosition.getHeight();
            int numberOfLines = (int) (Math.floor(previousTextYPosition - textYPosition) / height);
            numberOfLines = Math.max(1, numberOfLines - 1); // exclude current new line
            return numberOfLines;
        } else {
            return 0;
        }
    }

    private TextLine addNewLine() {
        final TextLine textLine = new TextLine(this.getCurrentPageWidth());
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
        final int index = character.getIndex();
        final char characterValue = character.getCharacterValue();
        if (this.indexIsInBounds(index) && this.line.charAt(index) == SPACE_CHARACTER) {
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
        final boolean isCharacterPartOfPreviousWord = character.isCharacterPartOfPreviousWord();
        final boolean isCharacterAtTheBeginningOfNewLine = character.isCharacterAtTheBeginningOfNewLine();
        final boolean isCharacterCloseToPreviousWord = character.isCharacterCloseToPreviousWord();

        if (!this.indexIsInBounds(index)) {
            return -1;
        } else {
            if (isCharacterPartOfPreviousWord && !isCharacterAtTheBeginningOfNewLine) {
                index = this.findMinimumIndexWithSpaceCharacterFromIndex(index);
            } else if (isCharacterCloseToPreviousWord) {
                if (this.line.charAt(index) != SPACE_CHARACTER) {
                    index = index + 1;
                } else {
                    index = this.findMinimumIndexWithSpaceCharacterFromIndex(index) + 1;
                }
            }
            index = this.getNextValidIndex(index, isCharacterPartOfPreviousWord);
            return index;
        }
    }

    private boolean isSpaceCharacterAtIndex(final int index) {
        return this.line.charAt(index) != SPACE_CHARACTER;
    }

    private boolean isNewIndexGreaterThanLastIndex(final int index) {
        final int lastIndex = this.getLastIndex();
        return (index > lastIndex);
    }

    private int getNextValidIndex(final int index, final boolean isCharacterPartOfPreviousWord) {
        int nextValidIndex = index;
        final int lastIndex = this.getLastIndex();
        if (!this.isNewIndexGreaterThanLastIndex(index)) {
            nextValidIndex = lastIndex + 1;
        }
        if (!isCharacterPartOfPreviousWord && this.isSpaceCharacterAtIndex(index - 1)) {
            nextValidIndex = nextValidIndex + 1;
        }
        this.setLastIndex(nextValidIndex);
        return nextValidIndex;
    }

    private int findMinimumIndexWithSpaceCharacterFromIndex(final int index) {
        int newIndex = index;
        while (newIndex >= 0 && this.line.charAt(newIndex) == SPACE_CHARACTER) {
            newIndex = newIndex - 1;
        }
        return newIndex + 1;
    }

    private boolean indexIsInBounds(final int index) {
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

    public Character(final char characterValue, final int index, final boolean isCharacterPartOfPreviousWord, final boolean isFirstCharacterOfAWord, final boolean isCharacterAtTheBeginningOfNewLine, final boolean isCharacterPartOfASentence) {
        this.characterValue = characterValue;
        this.index = index;
        this.isCharacterPartOfPreviousWord = isCharacterPartOfPreviousWord;
        this.isFirstCharacterOfAWord = isFirstCharacterOfAWord;
        this.isCharacterAtTheBeginningOfNewLine = isCharacterAtTheBeginningOfNewLine;
        this.isCharacterCloseToPreviousWord = isCharacterPartOfASentence;
        if (PDFLayoutTextStripper.DEBUG) {
            System.out.println(this.toString());
        }
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
        final char character = this.getCharacterFromTextPosition(textPosition);
        final int index = (int) textPosition.getX() / PDFLayoutTextStripper.OUTPUT_SPACE_CHARACTER_WIDTH_IN_PT;
        return new Character(character,
                index,
                isCharacterPartOfPreviousWord,
                isFirstCharacterOfAWord,
                isCharacterAtTheBeginningOfNewLine,
                isCharacterCloseToPreviousWord);
    }

    private boolean isCharacterAtTheBeginningOfNewLine(final TextPosition textPosition) {
        if (!firstCharacterOfLineFound) {
            return true;
        }
        final TextPosition previousTextPosition = this.getPreviousTextPosition();
        final double previousTextYPosition = previousTextPosition.getY();
        return (Math.round(textPosition.getY()) < Math.round(previousTextYPosition));
    }

    private boolean isFirstCharacterOfAWord(final TextPosition textPosition) {
        if (!firstCharacterOfLineFound) {
            return true;
        }
        final double numberOfSpaces = this.numberOfSpacesBetweenTwoCharacters(previousTextPosition, textPosition);
        return (numberOfSpaces > 1) || this.isCharacterAtTheBeginningOfNewLine(textPosition);
    }

    private boolean isCharacterCloseToPreviousWord(final TextPosition textPosition) {
        if (!firstCharacterOfLineFound) {
            return false;
        }
        final double numberOfSpaces = this.numberOfSpacesBetweenTwoCharacters(previousTextPosition, textPosition);
        final int widthOfSpace = (int) Math.ceil(textPosition.getWidthOfSpace());
        return (numberOfSpaces > 1 && numberOfSpaces <= widthOfSpace);
    }

    private boolean isCharacterPartOfPreviousWord(final TextPosition textPosition) {
        final TextPosition previousTextPosition = this.getPreviousTextPosition();
        if (previousTextPosition.getUnicode().equals(" ")) {
            return false;
        }
        final double numberOfSpaces = this.numberOfSpacesBetweenTwoCharacters(previousTextPosition, textPosition);
        return (numberOfSpaces <= 1);
    }

    private double numberOfSpacesBetweenTwoCharacters(final TextPosition textPosition1, final TextPosition textPosition2) {
        final double previousTextXPosition = textPosition1.getX();
        final double previousTextWidth = textPosition1.getWidth();
        final double previousTextEndXPosition = (previousTextXPosition + previousTextWidth);
        return (double) Math.abs(Math.round(textPosition2.getX() - previousTextEndXPosition));
    }

    private char getCharacterFromTextPosition(final TextPosition textPosition) {
        return textPosition.getUnicode().charAt(0);
    }

    private TextPosition getPreviousTextPosition() {
        return this.previousTextPosition;
    }

    private void setPreviousTextPosition(final TextPosition previousTextPosition) {
        this.previousTextPosition = previousTextPosition;
    }
}