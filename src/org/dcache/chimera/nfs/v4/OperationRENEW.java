package org.dcache.chimera.nfs.v4;

import org.dcache.chimera.nfs.v4.xdr.nfsstat4;
import org.dcache.chimera.nfs.v4.xdr.nfs_argop4;
import org.dcache.chimera.nfs.v4.xdr.nfs_opnum4;
import org.dcache.chimera.nfs.v4.xdr.RENEW4res;
import org.dcache.chimera.nfs.ChimeraNFSException;
import org.apache.log4j.Logger;

public class OperationRENEW extends AbstractNFSv4Operation {


	private static final Logger _log = Logger.getLogger(OperationRENEW.class.getName());

	OperationRENEW(nfs_argop4 args) {
		super(args, nfs_opnum4.OP_RENEW);
	}

	@Override
	public boolean process(CompoundContext context) {

        RENEW4res res = new RENEW4res();

        try {
            Long clientid = Long.valueOf(_args.oprenew.clientid.value.value);

            NFS4Client client = NFSv4StateHandler.getInstace().getClientByID( clientid );
            if( client == null ) {
                throw new ChimeraNFSException(nfsstat4.NFS4ERR_STALE_CLIENTID, "Bad client id");
            }

            client.updateLeaseTime(NFSv4Defaults.NFS4_LEASE_TIME);
            res.status = nfsstat4.NFS4_OK;

        }catch(ChimeraNFSException he) {
        	if(_log.isDebugEnabled() ) {
        		_log.debug("RENEW: " + he.getMessage() );
        	}
            res.status = he.getStatus();
        }


       _result.oprenew = res;

            context.processedOperations().add(_result);
            return res.status == nfsstat4.NFS4_OK;
	}

}