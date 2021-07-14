package io.agora.lrcview;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.agora.lrcview.bean.IEntry;
import io.agora.lrcview.bean.LrcData;
import io.agora.lrcview.bean.LrcEntryMigu;

/**
 * 工具类
 */
class LrcLoadMiguUtils {
    public static class Song {
        public SongGeneral general;
        public SongMidi midi;
    }

    public static class SongGeneral {
        public String name;
        public String singer;
        public int type;
        public String mode_type;
    }

    public static class SongMidi {
        public List<Paragraph> paragraphs;
    }

    public static class Paragraph {
        public List<IEntry> sentences;
    }

    /**
     * 从文件解析歌词
     */
    public static LrcData parseLrc(File lrcFile) {
        if (lrcFile == null || !lrcFile.exists()) {
            return null;
        }

        try (FileInputStream in = new FileInputStream(lrcFile)) {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            Song mSong = readLrc(parser);
            if (mSong == null || mSong.midi == null || mSong.midi.paragraphs == null) {
                return null;
            }

            LrcData mLrcData = new LrcData(IEntry.Type.Migu);
            List<IEntry> entrys = new ArrayList<>();
            for (Paragraph paragraph : mSong.midi.paragraphs) {
                entrys.addAll(paragraph.sentences);
            }
            mLrcData.setEntrys(entrys);
            return mLrcData;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static Song readLrc(XmlPullParser parser) throws XmlPullParserException, IOException {
        Song mSong = new Song();

        parser.require(XmlPullParser.START_TAG, null, "song");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("general")) {
                mSong.general = new SongGeneral();
                readGeneral(parser, mSong.general);
            } else if (name.equals("midi_lrc")) {
                mSong.midi = new SongMidi();
                readMidiLrc(parser, mSong.midi);
            } else {
                skip(parser);
            }
        }
        return mSong;
    }

    private static void readGeneral(XmlPullParser parser, SongGeneral general) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "general");

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("name")) {
                general.name = readText(parser);
            } else if (name.equals("singer")) {
                general.singer = readText(parser);
            } else if (name.equals("type")) {
                general.type = Integer.parseInt(readText(parser));
            } else if (name.equals("mode_type")) {
                general.mode_type = readText(parser);
            } else {
                skip(parser);
            }
        }
    }

    private static void readMidiLrc(XmlPullParser parser, SongMidi midi) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "midi_lrc");

        midi.paragraphs = new ArrayList<>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if (name.equals("paragraph")) {
                Paragraph mParagraph = new Paragraph();
                midi.paragraphs.add(mParagraph);
                readParagraph(parser, mParagraph);
            } else {
                skip(parser);
            }
        }
    }

    private static void readParagraph(XmlPullParser parser, Paragraph paragraph) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "paragraph");

        paragraph.sentences = new ArrayList<>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if (name.equals("sentence")) {
                LrcEntryMigu sentence = new LrcEntryMigu();
                paragraph.sentences.add(sentence);
                readSentence(parser, sentence);
            } else {
                skip(parser);
            }
        }
    }

    private static void readSentence(XmlPullParser parser, LrcEntryMigu sentence) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "sentence");

        sentence.tones = new ArrayList<>();
        sentence.mode = LrcEntryMigu.Mode.Default;
        String m = parser.getAttributeValue(null, "mode");
        if (m != null) {
            if (m.equals("man")) {
                sentence.mode = LrcEntryMigu.Mode.Man;
            } else {
                sentence.mode = LrcEntryMigu.Mode.Woman;
            }
        }

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if (name.equals("tone")) {
                LrcEntryMigu.Tone tone = new LrcEntryMigu.Tone();
                sentence.tones.add(tone);
                readTone(parser, tone);
            } else {
                skip(parser);
            }
        }
    }

    private static void readTone(XmlPullParser parser, LrcEntryMigu.Tone tone) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "tone");

        // read tone attributes
        tone.begin = Float.parseFloat(parser.getAttributeValue(null, "begin"));
        tone.end = Float.parseFloat(parser.getAttributeValue(null, "end"));
        String t = parser.getAttributeValue(null, "pitch");
        tone.pitch = 0;
        if (t != null) {
            tone.pitch = Integer.parseInt(t);
        }
        tone.pronounce = parser.getAttributeValue(null, "pronounce");
        tone.lang = parser.getAttributeValue(null, "lang");
        if (tone.lang == null) {
            tone.lang = "1";
        }

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("word")) {
                tone.word = readText(parser);
            } else {
                skip(parser);
            }
        }
    }

    private static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
