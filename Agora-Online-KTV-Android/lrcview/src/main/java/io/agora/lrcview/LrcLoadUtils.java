/*
 * Copyright (C) 2017 wangchenyan
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

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
