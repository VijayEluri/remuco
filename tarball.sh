#!/bin/sh

VERSION=$1

[ -z "$VERSION" ] && echo "need version" && exit 1

if [ -n "`svn st`" ] ; then
	echo
	echo "+-----------------------------------------------------------"
	echo "| WARNING: working copy has local changes"
	echo "+-----------------------------------------------------------"
	echo
	sleep 2
fi

# -----------------------------------------------------------------------------
# preparations
# -----------------------------------------------------------------------------

PKG=remuco-$VERSION

rm -rf tarballs build dist $PKG || exit 1

mkdir tarballs
mkdir $PKG

# -----------------------------------------------------------------------------
# update api.html
# -----------------------------------------------------------------------------

cd base/module
pydoc -w remuco
cd ../..

mv base/module/remuco.html doc/api.html

sed -i doc/api.html -e "s,[_a-z\.]\+\.html,api.html,g"

# -----------------------------------------------------------------------------
# base, adpaters, doc and top level files
# -----------------------------------------------------------------------------

for ITEM in base adapter doc setup.py Makefile ; do
	cp -r $ITEM $PKG/
done

# -----------------------------------------------------------------------------
# client
# -----------------------------------------------------------------------------

mkdir $PKG/client

cd client

for ITEM in src res design *.example build.xml setup.sh ; do
	cp -r $ITEM ../$PKG/client/
done

mkdir ../$PKG/client/app
ant dist
cp dist/remuco.jar dist/remuco.jad ../$PKG/client/app

cd ..

# -----------------------------------------------------------------------------
# clean up and package
# -----------------------------------------------------------------------------

find $PKG -type d -name ".svn" | xargs rm -rf
find $PKG -type f -name "*.pyc" | xargs rm -f
find $PKG -type f -name "*~" | xargs rm -f
find $PKG -type f -name "install*.log" | xargs rm -f

tar zcf tarballs/$PKG.tar.gz $PKG

# -----------------------------------------------------------------------------
# extra package: client source + binaries
# -----------------------------------------------------------------------------

PKG_CLIENT=remuco-client-$VERSION

rm -rf $PKG_CLIENT

cp -r $PKG $PKG_CLIENT

cd $PKG_CLIENT

ls -1 | grep -v "client" | xargs rm -rf
mv client/* .
rmdir client

cat > README << EOF
Remuco client sources and binaries.

Please visit http://remuco.sourceforge.net/index.php/Getting_Started for
installation and usage instructions.
EOF

cd ..

tar zcf tarballs/$PKG_CLIENT.tar.gz $PKG_CLIENT

# -----------------------------------------------------------------------------
# extra package: client JAR and JAD
# -----------------------------------------------------------------------------

cp $PKG_CLIENT/app/remuco.jar tarballs/remuco.jar
cp $PKG_CLIENT/app/remuco.jad tarballs/remuco.jad

# -----------------------------------------------------------------------------
# extra package: server source
# -----------------------------------------------------------------------------

PKG_SERVER=remuco-server-$VERSION

rm -rf $PKG_SERVER

cp -r $PKG $PKG_SERVER

rm -rf $PKG_SERVER/client

tar zcf tarballs/$PKG_SERVER.tar.gz $PKG_SERVER

# -----------------------------------------------------------------------------

rm -rf $PKG $PKG_CLIENT $PKG_SERVER
