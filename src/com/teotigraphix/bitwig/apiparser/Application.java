////////////////////////////////////////////////////////////////////////////////
// Copyright 2014 Michael Schmalle - Teoti Graphix, LLC
// 
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0 
// 
// Unless required by applicable law or agreed to in writing, software 
// distributed under the License is distributed on an "AS IS" BASIS, 
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and 
// limitations under the License
// 
// Author: Michael Schmalle, Principal Architect
// mschmalle at teotigraphix dot com
////////////////////////////////////////////////////////////////////////////////

package com.teotigraphix.bitwig.apiparser;

import java.io.File;
import java.io.IOException;

import com.teotigraphix.bitwig.apiparser.core.APIParser;
import com.teotigraphix.bitwig.apiparser.core.ParserConfig;

/**
 * @author Michael Schmalle
 * @since 1.0
 */
public class Application {

    // TODO command line configs
    private static final String SOURCE_DIR = "resources/src/com/bitwig/base/control_surface/iface";

    private static final String OUTPUT_DIR = "resources/output";

    /**
     * @param args
     */
    public static void main(String[] args) {
        ParserConfig config = new ParserConfig(SOURCE_DIR, OUTPUT_DIR, "API 1.1");
        config.setChangesFile(new File(OUTPUT_DIR, "BitwigStudio-API-Changes.html"));

        APIParser parser = new APIParser(config);
        parser.parse();

        try {
            parser.build();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
