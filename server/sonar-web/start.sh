#NOTES:
#check scripts/start.js for proxy = 'http://localhost:9033
# this server must be running

#COMMENT THIS BIT OUT IN config/webpack.config.js to make things go faster (but turn on and restart before releasing to do full checks)
#(!production || !fast) && {
#        test: /\.js$/,
#        enforce: 'pre',
#        include: paths.appSrc,
#        use: {
#          loader: 'eslint-loader',
#          options: { formatter: eslintFormatter }
#        }
#      }

export INSTANCE=CodeScanCloud
export PROXY='http://localhost:80'
./node/node scripts/start.js

