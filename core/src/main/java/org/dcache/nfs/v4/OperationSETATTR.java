/*
 * Copyright (c) 2009 - 2014 Deutsches Elektronen-Synchroton,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this program (see the file COPYING.LIB for more
 * details); if not, write to the Free Software Foundation, Inc.,
 * 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.dcache.nfs.v4;

import java.io.IOException;
import org.dcache.nfs.v4.xdr.int32_t;
import org.dcache.nfs.v4.xdr.utf8str_cs;
import org.dcache.nfs.v4.xdr.nfs4_prot;
import org.dcache.nfs.v4.xdr.bitmap4;
import org.dcache.nfs.v4.xdr.nfs_argop4;
import org.dcache.nfs.v4.xdr.fattr4_acl;
import org.dcache.nfs.nfsstat;
import org.dcache.nfs.v4.xdr.settime4;
import org.dcache.nfs.v4.xdr.fattr4;
import org.dcache.nfs.v4.xdr.time_how4;
import org.dcache.nfs.v4.xdr.nfstime4;
import org.dcache.nfs.v4.xdr.uint64_t;
import org.dcache.nfs.v4.xdr.mode4;
import org.dcache.nfs.v4.xdr.nfs_opnum4;
import org.dcache.nfs.v4.xdr.SETATTR4res;
import org.dcache.nfs.ChimeraNFSException;
import org.dcache.nfs.status.AttrNotSuppException;
import org.dcache.nfs.status.BadXdrException;
import org.dcache.nfs.status.InvalException;
import org.dcache.nfs.status.IsDirException;
import org.dcache.nfs.v4.acl.Acls;

import org.dcache.nfs.v4.xdr.nfs_resop4;
import org.dcache.xdr.XdrDecodingStream;
import org.dcache.nfs.vfs.Inode;
import org.dcache.nfs.vfs.Stat;
import org.dcache.xdr.BadXdrOncRpcException;
import org.dcache.xdr.OncRpcException;
import org.dcache.xdr.XdrBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OperationSETATTR extends AbstractNFSv4Operation {


    private static final Logger _log = LoggerFactory.getLogger(OperationSETATTR.class);

    OperationSETATTR(nfs_argop4 args) {
        super(args, nfs_opnum4.OP_SETATTR);
    }

    @Override
    public void process(CompoundContext context, nfs_resop4 result) throws ChimeraNFSException, IOException, OncRpcException {

        final SETATTR4res res = result.opsetattr;

        try {

            res.status = nfsstat.NFS_OK;
            res.attrsset = setAttributes(_args.opsetattr.obj_attributes, context.currentInode(), context);

        } catch (ChimeraNFSException e) {
            res.attrsset = new bitmap4(new int[] {0, 0});
            throw e;
        }
    }

    static bitmap4 setAttributes(fattr4 attributes, Inode inode, CompoundContext context) throws IOException, OncRpcException {

        XdrBuffer xdr = new XdrBuffer(attributes.attr_vals.value);
        xdr.beginDecoding();

        /*
         * bitmap we send back. can't be uninitialized.
         */
        bitmap4 processedAttributes = new bitmap4(new int[0]);
        Stat stat = new Stat();
        try {
            for (int i : attributes.attrmask) {
                if (xdr2fattr(i, stat, inode, context, xdr)) {
                    _log.debug("   setAttributes : {} ({}) OK", i, OperationGETATTR.attrMask2String(i));
                    processedAttributes.set(i);
                } else {
                    _log.debug("   setAttributes : {} ({}) NOT SUPPORTED", i, OperationGETATTR.attrMask2String(i));
                    throw new AttrNotSuppException("attribute " + OperationGETATTR.attrMask2String(i) + " not supported");
                }
            }
        }catch (BadXdrOncRpcException e) {
            throw new BadXdrException(e.getMessage());
        }

        if (xdr.hasMoreData()) {
            throw new BadXdrException("garbage in attr bitmap");
        }
        xdr.endDecoding();

        context.getFs().setattr(inode, stat);
        return processedAttributes;
    }

    static boolean xdr2fattr( int fattr , Stat stat, Inode inode, CompoundContext context, XdrDecodingStream xdr) throws IOException, OncRpcException {

        boolean isApplied = false;

        _log.debug("    FileAttribute: {}", fattr);

        switch(fattr) {

            case nfs4_prot.FATTR4_SIZE :
                uint64_t size = new uint64_t();
                size.xdrDecode(xdr);
                stat.setSize(size.value);
                isApplied = true;
                break;
            case nfs4_prot.FATTR4_ACL :
                fattr4_acl acl = new fattr4_acl();
                acl.xdrDecode(xdr);

                context.getFs().setAcl(inode, acl.value);
                stat.setMTime(System.currentTimeMillis());

                isApplied = true;
                break;
            case nfs4_prot.FATTR4_ARCHIVE :
                int32_t isArchive = new int32_t();
                isArchive.xdrDecode(xdr);
                isApplied = false;
                break;
            case nfs4_prot.FATTR4_HIDDEN :
                int32_t isHidden = new int32_t();
                isHidden.xdrDecode(xdr);
                isApplied = false;
                break;
            case nfs4_prot.FATTR4_MIMETYPE :
                utf8str_cs mimeType = new utf8str_cs();
                mimeType.xdrDecode(xdr);
                isApplied = false;
                break;
            case nfs4_prot.FATTR4_MODE :
                mode4 mode = new mode4();
                mode.xdrDecode(xdr);
                stat.setMode(mode.value);
                context.getFs().setAcl(inode, Acls.adjust( context.getFs().getAcl(inode), mode.value));
                isApplied = true;
                break;
            case nfs4_prot.FATTR4_OWNER :
                // TODO: use princilat
                utf8str_cs owner = new utf8str_cs ();
                owner.xdrDecode(xdr);
                String new_owner = owner.toString();
                stat.setUid(context.getFs().getIdMapper().principalToUid(new_owner));
                isApplied = true;
                break;
            case nfs4_prot.FATTR4_OWNER_GROUP :
                // TODO: use princilat
                utf8str_cs owner_group = new utf8str_cs ();
                owner_group.xdrDecode(xdr);
                String new_group = owner_group.toString();
                stat.setGid(context.getFs().getIdMapper().principalToGid(new_group));
                isApplied = true;
                break;
            case nfs4_prot.FATTR4_SYSTEM :
                int32_t isSystem = new int32_t();
                isSystem.xdrDecode(xdr);
                isApplied = false;
                break;
            case nfs4_prot.FATTR4_TIME_ACCESS_SET :
                settime4 atime = new settime4();
                atime.xdrDecode(xdr);
                // ignore for performance
                isApplied = true;
                break;
            case nfs4_prot.FATTR4_TIME_BACKUP :
                nfstime4 btime = new nfstime4();
                btime.xdrDecode(xdr);
                isApplied = false;
                break;
            case nfs4_prot.FATTR4_TIME_CREATE :
                nfstime4 ctime = new nfstime4();
                ctime.xdrDecode(xdr);
                stat.setCTime(ctime.toMillis());
                isApplied = true;
                break;
            case nfs4_prot.FATTR4_TIME_MODIFY_SET :
                settime4 setMtime = new settime4();
                setMtime.xdrDecode(xdr);

                long realMtime = setMtime.set_it == time_how4.SET_TO_SERVER_TIME4 ?
                    System.currentTimeMillis() : setMtime.time.toMillis();

                stat.setMTime(realMtime);
                isApplied = true;
                break;
            case nfs4_prot.FATTR4_SUPPORTED_ATTRS:
                throw new InvalException("setattr of read-only attributes");
        }

        if(!isApplied ) {
            _log.info("Attribute not applied: {}", OperationGETATTR.attrMask2String(fattr) );
        }
        return isApplied;
    }

}
