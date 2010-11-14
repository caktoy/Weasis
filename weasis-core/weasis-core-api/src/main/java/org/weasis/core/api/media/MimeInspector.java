/*******************************************************************************
 * Copyright (c) 2010 Nicolas Roduit.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Nicolas Roduit - initial API and implementation
 ******************************************************************************/
package org.weasis.core.api.media;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.weasis.core.api.Messages;
import org.weasis.core.api.internal.mime.InvalidMagicMimeEntryException;
import org.weasis.core.api.internal.mime.MagicMimeEntry;
import org.weasis.core.api.util.FileUtil;

public class MimeInspector {

    public static final Icon textIcon =
        new ImageIcon(MimeInspector.class.getResource("/icon/22x22/text-x-generic.svg")); //$NON-NLS-1$
    public static final Icon imageIcon =
        new ImageIcon(MimeInspector.class.getResource("/icon/22x22/image-x-generic.png")); //$NON-NLS-1$
    public static final Icon audioIcon =
        new ImageIcon(MimeInspector.class.getResource("/icon/22x22/audio-x-generic.png")); //$NON-NLS-1$
    public static final Icon videoIcon =
        new ImageIcon(MimeInspector.class.getResource("/icon/22x22/video-x-generic.png")); //$NON-NLS-1$
    public static final Icon dicomIcon = new ImageIcon(MimeInspector.class.getResource("/icon/22x22/dicom.png")); //$NON-NLS-1$
    public static final Icon dicomVideo = new ImageIcon(MimeInspector.class.getResource("/icon/22x22/dicom-video.png")); //$NON-NLS-1$
    public static final String UNKNOWN_MIME_TYPE = "application/x-unknown-mime-type"; //$NON-NLS-1$
    private static Properties mimeTypes;

    private static ArrayList<MagicMimeEntry> mMagicMimeEntries = new ArrayList<MagicMimeEntry>();
    // Initialise the class in preperation for mime type detection
    static {
        mimeTypes = new Properties();
        InputStream fileStream = null;
        try {
            // Load the default supplied mime types
            fileStream = MimeInspector.class.getResourceAsStream("/mime-types.properties"); //$NON-NLS-1$
            mimeTypes.load(fileStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            FileUtil.safeClose(fileStream);
        }

        // Parse and initialize the magic.mime rules
        InputStream is = MimeInspector.class.getResourceAsStream("/magic.mime"); //$NON-NLS-1$
        if (is != null) {
            try {
                parse(new InputStreamReader(is, "UTF8")); //$NON-NLS-1$
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                FileUtil.safeClose(is);
            }
        }
    }

    public static String getMimeType(final File file) throws IOException {
        if (file == null || !file.canRead()) {
            return null;
        }

        // Get the file extension
        String fileName = file.getName();
        int lastPos = fileName.lastIndexOf("."); //$NON-NLS-1$
        String extension = lastPos > 0 ? fileName.substring(lastPos + 1) : null;

        String mimeType = null;

        if (extension != null && extension.trim().length() > 0) {
            mimeType = mimeTypes.getProperty(extension.toLowerCase());

        }
        if (mimeType == null) {
            mimeType = MimeInspector.getMagicMimeType(file);
        }

        return mimeType;
    }

    private static void parse(Reader r) throws IOException {
        BufferedReader br = new BufferedReader(r);
        String line;
        ArrayList<String> sequence = new ArrayList<String>();

        line = br.readLine();
        while (true) {
            if (line == null) {
                break;
            }
            line = line.trim();
            if (line.length() == 0 || line.charAt(0) == '#') {
                line = br.readLine();
                continue;
            }
            sequence.add(line);

            // read the following lines until a line does not begin with '>' or EOF
            while (true) {
                line = br.readLine();
                if (line == null) {
                    addEntry(sequence);
                    sequence.clear();
                    break;
                }
                line = line.trim();
                if (line.length() == 0 || line.charAt(0) == '#') {
                    continue;
                }
                if (line.charAt(0) != '>') {
                    addEntry(sequence);
                    sequence.clear();
                    break;
                }
                sequence.add(line);
            }

        }
        if (!sequence.isEmpty()) {
            addEntry(sequence);
        }
    }

    private static void addEntry(ArrayList<String> aStringArray) {
        try {
            MagicMimeEntry magicEntry = new MagicMimeEntry(aStringArray);
            mMagicMimeEntries.add(magicEntry);
        } catch (InvalidMagicMimeEntryException e) {
            // Continue on but lets print an exception so people can see there is a problem
            e.printStackTrace();
        }
    }

    private static String getMagicMimeType(File f) throws IOException {
        if (f.isDirectory()) {
            return "application/directory"; //$NON-NLS-1$
        }
        int len = mMagicMimeEntries.size();
        RandomAccessFile raf = new RandomAccessFile(f, "r"); //$NON-NLS-1$
        for (int i = 0; i < len; i++) {
            MagicMimeEntry me = mMagicMimeEntries.get(i);
            String mtype = me.getMatch(raf);
            if (mtype != null) {
                return mtype;
            }
        }
        return null;
    }

    // Utility method to get the major part of a mime type
    public static String getMajorComponent(String mimeType) {
        if (mimeType == null) {
            return ""; //$NON-NLS-1$
        }
        int offset = mimeType.indexOf("/"); //$NON-NLS-1$
        if (offset == -1) {
            return mimeType;
        } else {
            return mimeType.substring(0, offset);
        }
    }

    // Utility method to get the minor part of a mime type
    public static String getMinorComponent(String mimeType) {
        if (mimeType == null) {
            return ""; //$NON-NLS-1$
        }
        int offset = mimeType.indexOf("/"); //$NON-NLS-1$
        if (offset == -1) {
            return mimeType;
        } else {
            return mimeType.substring(offset + 1);
        }
    }

    // Utility method that gets the extension of a file from its name if it has one
    public static String getFileExtension(String fileName) {
        int lastPos;
        if (fileName == null || (lastPos = fileName.lastIndexOf(".")) < 0) { //$NON-NLS-1$
            return null;
        }
        String extension = fileName.substring(lastPos + 1);
        // Could be that the path actually had a '.' in it so lets check
        if (extension.contains(File.separator)) {
            return null;
        }
        return extension;
    }

    public static String[] getExtensions(String mime) {
        Set<Entry<Object, Object>> entries = mimeTypes.entrySet();
        ArrayList<String> list = new ArrayList<String>();
        for (Entry<Object, Object> entry : entries) {
            String val = (String) entry.getValue();
            if (val != null) {
                String[] mimeTypes = mime.split(","); //$NON-NLS-1$
                for (String mimeType : mimeTypes) {
                    if (mimeType.equals(mime)) {
                        String key = (String) entry.getKey();
                        if (!list.contains(key)) {
                            list.add(key);
                        }
                    }
                }
            }
        }
        return list.toArray(new String[list.size()]);
    }
}