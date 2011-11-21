/*
 * Automatically generated by jrpcgen 1.0.7 on 2/21/09 1:22 AM
 * jrpcgen is part of the "Remote Tea" ONC/RPC package for Java
 * See http://remotetea.sourceforge.net for details
 */
package org.dcache.chimera.nfs.v4.xdr;
import org.dcache.chimera.nfs.nfsstat;
import org.dcache.chimera.nfs.v4.*;
import org.dcache.xdr.*;
import java.io.IOException;

public class LAYOUTGET4res implements XdrAble {
    public int logr_status;
    public LAYOUTGET4resok logr_resok4;
    public boolean logr_will_signal_layout_avail;

    public LAYOUTGET4res() {
    }

    public LAYOUTGET4res(XdrDecodingStream xdr)
           throws OncRpcException, IOException {
        xdrDecode(xdr);
    }

    public void xdrEncode(XdrEncodingStream xdr)
           throws OncRpcException, IOException {
        xdr.xdrEncodeInt(logr_status);
        switch ( logr_status ) {
        case nfsstat.NFS_OK:
            logr_resok4.xdrEncode(xdr);
            break;
        case nfsstat.NFSERR_LAYOUTTRYLATER:
            xdr.xdrEncodeBoolean(logr_will_signal_layout_avail);
            break;
        default:
            break;
        }
    }

    public void xdrDecode(XdrDecodingStream xdr)
           throws OncRpcException, IOException {
        logr_status = xdr.xdrDecodeInt();
        switch ( logr_status ) {
        case nfsstat.NFS_OK:
            logr_resok4 = new LAYOUTGET4resok(xdr);
            break;
        case nfsstat.NFSERR_LAYOUTTRYLATER:
            logr_will_signal_layout_avail = xdr.xdrDecodeBoolean();
            break;
        default:
            break;
        }
    }

}
// End of LAYOUTGET4res.java
