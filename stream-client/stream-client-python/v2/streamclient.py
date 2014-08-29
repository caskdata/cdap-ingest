#! /usr/bin/env python2
# -*- coding: utf-8 -*-

import json
from config import Config
from serviceconnector import ServiceConnector, ConnectionErrorChecker
from streamwriter import StreamWriter


class StreamClient(ConnectionErrorChecker):

    __serviceConnector = None
    __serviceConfig = None

    __GATEWAY_VERSION = u'/v2'
    __REQUEST_PLACEHOLDERS = {
        u'streamid': u'<streamid>'
    }
    __REQUESTS = {u'streams': __GATEWAY_VERSION + u'/streams'}
    __REQUESTS[u'stream'] = u'{0}/{1}'.format(__REQUESTS[u'streams'],
                                            __REQUEST_PLACEHOLDERS[u'streamid'])
    __REQUESTS[u'consumerid'] = u'{0}/{1}'.format(__REQUESTS[u'stream'],
                                                u'consumer-id')
    __REQUESTS[u'dequeue'] = u'{0}/{1}'.format(__REQUESTS[u'stream'], u'dequeue')
    __REQUESTS[u'config'] = u'{0}/{1}'.format(__REQUESTS[u'stream'], u'config')
    __REQUESTS[u'info'] = u'{0}/{1}'.format(__REQUESTS[u'stream'], u'info')
    __REQUESTS[u'truncate'] = u'{0}/{1}'.format(__REQUESTS[u'stream'], u'truncate')

    def __init__(self, config=Config()):
        self.__serviceConfig = config
        self.__serviceConnector = ServiceConnector(self.__serviceConfig)

    def __prepareUri(self, requestName, placeholderName=u'streamid', data=u''):
        return self.__REQUESTS[requestName].replace(
            self.__REQUEST_PLACEHOLDERS[placeholderName], data)

    def create(self, stream):
        u"""
        Creates a stream with the given name.

        Keyword arguments:
        stream -- stream name to create
        """
        uri = self.__prepareUri(u'stream', data=stream)

        self.__serviceConnector.request(u'PUT', uri)

    def setTTL(self, stream, ttl):
        u"""
        Set the Time-To-Live (TTL) property of the given stream.

        Keyword arguments:
        stream -- stream name to create or to retrieve
        ttl -- Time-To-Live in seconds
        """
        objectToSend = {
            u'ttl': ttl
        }
        uri = self.__prepareUri(u'config', data=stream)
        data = json.dumps(objectToSend)

        self.checkResponseErrors(
            self.__serviceConnector.request(u'PUT', uri, data)
        )

    def getTTL(self, stream):
        u"""
        Retrieves the Time-To-Live (TTL) property of the given stream.

        Keyword arguments:
        stream -- stream name to retrieve ttl for

        Return value:
        Time-To-Live property in seconds
        """
        uri = self.__prepareUri(u'info', data=stream)
        response = self.checkResponseErrors(
            self.__serviceConnector.request(u'GET', uri)
        )

        ttl = response.json()[u'ttl']

        return ttl

    def truncate(self, stream):
        u"""
        Truncates all existing events in the give stream.

        Keyword arguments:
        stream -- stream name to truncate
        """
        uri = self.__prepareUri(u'truncate', data=stream)

        self.checkResponseErrors(
            self.__serviceConnector.request(u'POST', uri)
        )

    def createWriter(self, stream):
        u"""
        Creates a {@link StreamWriter} instance for writing events
        to the given stream.

        Keyword arguments:
        stream -- stream name to get StreamWrite instance for
        """

        u"""
        A bit ugly, but effective method to check if stream exists.
        The main idea is there is could not be presented info for
        invalid stream.
        """
        self.getTTL(stream)

        uri = self.__prepareUri(u'stream', data=stream)

        return StreamWriter(
            ServiceConnector(self.__serviceConfig),
            uri
        )
