/**
 * 
 */
package org.broad.igv.sam;

import java.awt.Color;

import org.broad.igv.sam.AlignmentTrack.BisulfiteContext;

/**
 * @author benb
 *
 */
public class BisulfiteBaseInfoNOMeseq extends BisulfiteBaseInfo {

	
	public static Color GC_METH_COLOR = new Color(0,204,153);
	public static Color GC_UNMETH_COLOR = new Color(255,127,191);
	public static Color CG_METH_COLOR = Color.black;
	public static Color CG_UNMETH_COLOR = Color.white;
	
	
	/**
	 * @param inReference
	 * @param inRead
	 * @param alignmentLen
	 * @param block
	 * @param bisulfiteContext
	 */
	public BisulfiteBaseInfoNOMeseq(byte[] inReference, byte[] inRead,
			int alignmentLen, AlignmentBlock block,
			BisulfiteContext bisulfiteContext) {
		super(inReference, inRead, alignmentLen, block, bisulfiteContext);
	}


	@Override
	protected Color getContextColor(byte readbase,
			BisulfiteContext bisulfiteContext) {
		
		Color out = null;
		if (bisulfiteContext.equals(BisulfiteContext.HCG))
		{
			out = (AlignmentRenderer.compareBases((byte)'T',readbase)) ? CG_UNMETH_COLOR : CG_METH_COLOR;
		}
		else
		{
			out = (AlignmentRenderer.compareBases((byte)'T',readbase)) ? GC_UNMETH_COLOR : GC_METH_COLOR;
		}
		return out;
	}


	@Override
	protected BisulfiteContext contextIsMatching(byte[] reference, byte[] read, int idx,
			BisulfiteContext bisulfiteContext) {

		for (BisulfiteContext context : new BisulfiteContext[]{ BisulfiteContext.HCG, BisulfiteContext.GCH })
		{
			if (super.contextIsMatching(reference, read, idx,context) != null) return context;
		}
		return null;
	}


	@Override
	protected double getBisulfiteSymmetricCytosineShift(BisulfiteContext item) {
		// TODO Auto-generated method stub
		return 0.0;
	}
	
	
	

}