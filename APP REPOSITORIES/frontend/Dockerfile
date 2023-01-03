FROM nginx:latest
ENV JSFOLDER=/usr/share/nginx/html/*.js
# ENV API_URL=example.com
COPY dist/training-license-fe /usr/share/nginx/html

RUN printf '#!/usr/bin/env bash\nexport EXISTING_VARS=$(printenv | awk -F= "{print $1}" | sed "s/^/\$/g" | paste -sd,);\nfor file in $JSFOLDER;\ndo\n    envsubst "$EXISTING_VARS" < $file > "$file.temp"\n    rm $file\n    mv "$file.temp" $file\ndone\nnginx -g "daemon off;"\n' > /usr/bin/start-nginx.sh
RUN chmod +x /usr/bin/start-nginx.sh
ENTRYPOINT [ "start-nginx.sh" ]

EXPOSE 80
