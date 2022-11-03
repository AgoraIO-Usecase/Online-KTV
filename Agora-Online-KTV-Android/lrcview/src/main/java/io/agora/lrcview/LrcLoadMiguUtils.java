package io.agora.lrcview;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.agora.lrcview.bean.LrcData;
import io.agora.lrcview.bean.LrcEntryData;

/**
 * 咪咕加载xml歌词
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/7/6
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
        public List<LrcEntryData> sentences;
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

            LrcData mLrcData = new LrcData(LrcData.Type.Migu);
            List<LrcEntryData> entrys = new ArrayList<>();
            for (Paragraph paragraph : mSong.midi.paragraphs) {
                entrys.addAll(paragraph.sentences);
            }
            mLrcData.entrys = entrys;
            return mLrcData;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static Song readLrc(XmlPullParser parser) throws XmlPullParserException, IOException {
        Song mSong = new Song();
//        parser.require(XmlPullParser.START_TAG, null, "song");
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
//            } else if (name.equals("type")) {
//                general.type = Integer.parseInt(readText(parser));
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
                List<LrcEntryData> sentence = new ArrayList<>();
                readSentence(parser, sentence);
                for (LrcEntryData item : sentence) {
                    paragraph.sentences.add(item);
                }
            } else {
                skip(parser);
            }
        }
    }

    private static void readSentence(XmlPullParser parser, List<LrcEntryData> list) throws XmlPullParserException, IOException {
        LrcEntryData sentence = new LrcEntryData(new ArrayList<>());
        list.add(sentence);
        parser.require(XmlPullParser.START_TAG, null, "sentence");
        String m = parser.getAttributeValue(null, "mode");
        if (m != null) {
            if (m.equals("man")) {
                // Man;
            } else {
                // Woman;
            }
        }

        int tone_index = 0;

        boolean isEnglish = false;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if (name.equals("tone")) {
                tone_index++;
                if ((isEnglish || isEnglishSong(parser)) && tone_index > 5) {
                    sentence = new LrcEntryData(new ArrayList<>());
                    list.add(sentence);
                    tone_index = 0;
                }
                LrcEntryData.Tone tone = new LrcEntryData.Tone();
                sentence.tones.add(tone);
                isEnglish = readTone(parser, tone);
            } else if (name.equals("monolog")) {
                tone_index++;
                if ((isEnglish || isEnglishSong(parser)) && tone_index > 5) {
                    sentence = new LrcEntryData(new ArrayList<>());
                    list.add(sentence);
                    tone_index = 0;
                }
                LrcEntryData.Monolog monolog = new LrcEntryData.Monolog();
                sentence.tones.add(monolog);
                isEnglish = readMonolog(parser, monolog);
            } else {
                skip(parser);
            }
        }
    }

    private static boolean isEnglishSong(XmlPullParser parser) {
        String lang = parser.getAttributeValue(null, "lang");
        return lang != null && !"1".equals(lang);
    }

    private static boolean readTone(XmlPullParser parser, LrcEntryData.Tone tone) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "tone");

        boolean isEnglish = false;

        // read tone attributes
        tone.begin = (long) (Float.parseFloat(parser.getAttributeValue(null, "begin")) * 1000L);
        tone.end = (long) (Float.parseFloat(parser.getAttributeValue(null, "end")) * 1000L);
        String t = parser.getAttributeValue(null, "pitch");
        int pitch = 0;
        if (t != null) {
            pitch = Integer.parseInt(t);
        }
        tone.pitch = pitch;

        String pronounce = parser.getAttributeValue(null, "pronounce");
        String lang = parser.getAttributeValue(null, "lang");

        if (lang == null || "1".equals(lang)) {
            tone.lang = LrcEntryData.Lang.Chinese;
        } else {
            tone.lang = LrcEntryData.Lang.English;
            isEnglish = true;
        }

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("word")) {
                tone.word = readText(parser);
                // protect in case migu missed lang field
                if (lang == null) {
                    isEnglish = checkLang(tone.word);
                    if (isEnglish) {
                        tone.lang = LrcEntryData.Lang.English;
                    }
                }
            } else {
                skip(parser);
            }
        }
        return isEnglish;
    }

    private static boolean readMonolog(XmlPullParser parser, LrcEntryData.Monolog monolog) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "monolog");

        boolean isEnglish = false;

        // read tone attributes
        monolog.begin = (long) (Float.parseFloat(parser.getAttributeValue(null, "begin")) * 1000L);
        monolog.end = (long) (Float.parseFloat(parser.getAttributeValue(null, "end")) * 1000L);
        String t = parser.getAttributeValue(null, "pitch");
        int pitch = 0;
        if (t != null) {
            pitch = Integer.parseInt(t);
        }
        monolog.pitch = pitch;

        String pronounce = parser.getAttributeValue(null, "pronounce");
        String lang = parser.getAttributeValue(null, "lang");

        if (lang == null || "1".equals(lang)) {
            monolog.lang = LrcEntryData.Lang.Chinese;
        } else {
            monolog.lang = LrcEntryData.Lang.English;
            isEnglish = true;
        }

        monolog.word = readText(parser);
        return isEnglish;
    }

    private static boolean checkLang(String word) {
        int n;
        for (int i = 0; i < word.length(); i++) {
            n = word.charAt(i);
            if (!(19968 <= n && n < 40869)) {
                return true;
            }
        }
        return false;
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
