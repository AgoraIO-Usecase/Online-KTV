package io.agora.lrcview;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.agora.lrcview.bean.IEntry;
import io.agora.lrcview.bean.LrcData;

class LrcLoadUtils {

    private static final Executor mExecutor = Executors.newSingleThreadExecutor();

    public static void execute(Runnable runnable) {
        mExecutor.execute(runnable);
    }

    @Nullable
    public static LrcData parse(File lrcFile) {
        return parse(null, lrcFile);
    }

    @Nullable
    public static LrcData parse(IEntry.Type type, File lrcFile) {
        if (type == null) {
            try {
                InputStream instream = new FileInputStream(lrcFile);
                InputStreamReader inputreader = new InputStreamReader(instream);
                BufferedReader buffreader = new BufferedReader(inputreader);
                String line = buffreader.readLine();
                if (line.contains("xml")) {
                    type = IEntry.Type.Migu;
                } else {
                    type = IEntry.Type.Default;
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (type == IEntry.Type.Default) {
            return LrcLoadDefaultUtils.parseLrc(lrcFile);
        } else if (type == IEntry.Type.Migu) {
            return LrcLoadMiguUtils.parseLrc(lrcFile);
        } else {
            return null;
        }
    }
}
