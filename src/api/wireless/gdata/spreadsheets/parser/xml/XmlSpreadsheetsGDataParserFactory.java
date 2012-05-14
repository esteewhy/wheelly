/*******************************************************************************
 * Copyright 2009 Art Wild
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package api.wireless.gdata.spreadsheets.parser.xml;

import api.wireless.gdata.spreadsheets.data.ListEntry;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import com.google.android.apps.mytracks.io.gdata.docs.SpreadsheetsClient.SpreadsheetEntry;
import com.google.android.apps.mytracks.io.gdata.docs.XmlDocsGDataParserFactory;
import com.google.wireless.gdata.data.Entry;
import com.google.wireless.gdata.parser.GDataParser;
import com.google.wireless.gdata.parser.ParseException;
import com.google.wireless.gdata.parser.xml.XmlParserFactory;
import com.google.wireless.gdata.serializer.GDataSerializer;

import java.io.InputStream;

/**
 * A GDataParserFactory capable of handling Spreadsheets.
 */
public class XmlSpreadsheetsGDataParserFactory extends XmlDocsGDataParserFactory {
    /*
     * @see GDataParserFactory
     */
    public XmlSpreadsheetsGDataParserFactory(XmlParserFactory xmlFactory) {
    	super(xmlFactory);
        this.xmlFactory = xmlFactory;
    }

    /*
     * Creates a parser for the indicated feed, assuming the default feed type.
     * The default type is specified on {@link SpreadsheetsClient#DEFAULT_FEED}.
     * 
     * @param is The stream containing the feed to be parsed.
     * @return A GDataParser capable of parsing the feed as the default type.
     * @throws ParseException if the feed could not be parsed for any reason
     */
    public GDataParser createParser(InputStream is) throws ParseException {
        // attempt a default
        return createParser(SpreadsheetEntry.class, is);
    }

    /*
     * Creates a parser of the indicated type for the indicated feed.
     * 
     * @param feedType The type of the feed; must be one of the constants on
     *        {@link SpreadsheetsClient}.
     * @return A parser capable of parsing the feed as the indicated type.
     * @throws ParseException if the feed could not be parsed for any reason
     */
    @SuppressWarnings("rawtypes")
	public GDataParser createParser(Class entryClass, InputStream is)
            throws ParseException {
    	if (entryClass == ListEntry.class) {
	    	try {
	            XmlPullParser xmlParser = xmlFactory.createParser();
	            return new XmlListGDataParser(is, xmlParser);
	        } catch (XmlPullParserException e) {
	            throw new ParseException("Failed to create parser", e);
	        }
        } else {
            return super.createParser(entryClass, is);
        }
    }

    /*
     * Creates a serializer capable of handling the indicated entry.
     * 
     * @param The Entry to be serialized to an XML string.
     * @return A GDataSerializer capable of handling the indicated entry.
     * @throws IllegalArgumentException if Entry is not a supported type (which
     *         currently includes only {@link ListEntry} and {@link CellEntry}.)
     */
    public GDataSerializer createSerializer(Entry entry) {
    	return super.createSerializer(entry);
    }

    /** The XmlParserFactory to use to actually process XML streams. */
    private XmlParserFactory xmlFactory;
}