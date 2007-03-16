//
// SEQReader.java
//

/*
LOCI Bio-Formats package for reading and converting biological file formats.
Copyright (C) 2005-@year@ Melissa Linkert, Curtis Rueden, Chris Allan,
Eric Kjellman and Brian Loranger.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU Library General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Library General Public License for more details.

You should have received a copy of the GNU Library General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package loci.formats.in;

import java.io.IOException;
import java.util.StringTokenizer;
import loci.formats.*;

/**
 * SEQReader is the file format reader for Image-Pro Sequence files.
 *
 * @author Melissa Linkert linkert at wisc.edu
 */
public class SEQReader extends BaseTiffReader {

  /** Number of optical sections in the file */
  private int zSize = 0;

  /** Number of timepoints in the file */
  private int tSize = 1;

  // -- Constants --

  /**
   * An array of shorts (length 12) with identical values in all of our
   * samples; assuming this is some sort of format identifier.
   */
  private static final int IMAGE_PRO_TAG_1 = 50288;

  /** Frame rate. */
  private static final int IMAGE_PRO_TAG_2 = 40105;

  // -- Constructor --

  /** Constructs a new Image-Pro SEQ reader. */
  public SEQReader() { super("Image-Pro Sequence", "seq"); }

  // -- Internal BaseTiffReader API methods --

  /** Overridden to include the three SEQ-specific tags. */
  protected void initStandardMetadata() throws FormatException, IOException {
    super.initStandardMetadata();

    for (int j=0; j<ifds.length; j++) {
      short[] tag1 = (short[]) TiffTools.getIFDValue(ifds[j], IMAGE_PRO_TAG_1);

      if (tag1 != null) {
        String seqId = "";
        for (int i=0; i<tag1.length; i++) seqId = seqId + tag1[i];
        addMeta("Image-Pro SEQ ID", seqId);
      }

      int tag2 = TiffTools.getIFDIntValue(ifds[0], IMAGE_PRO_TAG_2);

      if (tag2 != -1) {
        // should be one of these for every image plane
        zSize++;
        addMeta("Frame Rate", new Integer(tag2));
      }

      addMeta("Number of images", new Integer(zSize));
    }

    if (zSize == 0) zSize++;

    if (zSize == 1 && tSize == 1) {
      zSize = ifds.length;
    }

    // default values
    addMeta("frames", "" + zSize);
    addMeta("channels", getMeta("NumberOfChannels").toString());
    addMeta("slices", "" + tSize);

    // parse the description to get channels, slices and times where applicable
    String descr = (String) getMeta("Comment");
    metadata.remove("Comment");
    if (descr != null) {
      StringTokenizer tokenizer = new StringTokenizer(descr, "\n");
      while (tokenizer.hasMoreTokens()) {
        String token = tokenizer.nextToken();
        String label = token.substring(0, token.indexOf("="));
        String data = token.substring(token.indexOf("=") + 1);
        addMeta(label, data);
      }
    }

    sizeC[0] = Integer.parseInt((String) getMeta("channels"));
    sizeZ[0] = Integer.parseInt((String) getMeta("frames"));
    sizeT[0] = Integer.parseInt((String) getMeta("slices"));

    try {
      if (isRGB(currentId) && sizeC[0] != 3) sizeC[0] *= 3;
    }
    catch (IOException e) {
      throw new FormatException(e);
    }

    currentOrder[0] = "XY";

    int maxNdx = 0, max = 0;
    int[] dims = {sizeZ[0], sizeC[0], sizeT[0]};
    String[] axes = {"Z", "C", "T"};

    for (int i=0; i<dims.length; i++) {
      if (dims[i] > max) {
        max = dims[i];
        maxNdx = i;
      }
    }

    currentOrder[0] += axes[maxNdx];

    if (maxNdx != 1) {
      if (sizeC[0] > 1) {
        currentOrder[0] += "C";
        currentOrder[0] += (maxNdx == 0 ? axes[2] : axes[0]);
      }
      else currentOrder[0] += (maxNdx == 0 ? axes[2] : axes[0]) + "C";
    }
    else {
      if (sizeZ[0] > sizeT[0]) currentOrder[0] += "ZT";
      else currentOrder[0] += "TZ";
    }
  }

  // -- Main method --

  public static void main(String[] args) throws FormatException, IOException {
    new SEQReader().testRead(args);
  }

}
