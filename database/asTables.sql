-- phpMyAdmin SQL Dump
-- version 3.3.9.2
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generato il: 26 set, 2012 at 04:07 PM
-- Versione MySQL: 5.5.9
-- Versione PHP: 5.3.6

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";

--
-- Database: `semlibStorage`
--

-- --------------------------------------------------------

--
-- Struttura della tabella `activenotebooks`
--

DROP TABLE IF EXISTS `activenotebooks`;
CREATE TABLE IF NOT EXISTS `activenotebooks` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `userid` varchar(40) NOT NULL,
  `notebookid` varchar(40) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `userid` (`userid`,`notebookid`),
  KEY `notebookid` (`notebookid`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=584 ;

-- --------------------------------------------------------

--
-- Struttura della tabella `admins`
--

DROP TABLE IF EXISTS `admins`;
CREATE TABLE IF NOT EXISTS `admins` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(40) NOT NULL,
  `password` varchar(40) NOT NULL,
  `openid` varchar(255) NOT NULL,
  `firstname` varchar(40) NOT NULL,
  `lastname` varchar(40) NOT NULL,
  `email` varchar(40) NOT NULL,
  `role` tinyint(4) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `username` (`username`,`password`,`openid`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=2 ;

--
-- Dump dei dati per la tabella `admins`
--

INSERT INTO `admins` (`id`, `username`, `password`, `openid`, `firstname`, `lastname`, `email`, `role`) VALUES(1, 'admin', '2b37ddb1972061b91f24f3e1b9d84c66ad00dd14', '', 'Pundit Super Administrator', '', '', 0);

-- --------------------------------------------------------

--
-- Struttura della tabella `annotations`
--

DROP TABLE IF EXISTS `annotations`;
CREATE TABLE IF NOT EXISTS `annotations` (
  `annotationid` varchar(40) NOT NULL,
  `notebookid` varchar(40) NOT NULL,
  PRIMARY KEY (`annotationid`),
  KEY `notebookId` (`notebookid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Struttura della tabella `currentnotebooks`
--

DROP TABLE IF EXISTS `currentnotebooks`;
CREATE TABLE IF NOT EXISTS `currentnotebooks` (
  `userid` varchar(40) NOT NULL,
  `currentnotebook` varchar(40) NOT NULL,
  PRIMARY KEY (`userid`),
  KEY `currentnotebook` (`currentnotebook`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Struttura della tabella `notebooks`
--

DROP TABLE IF EXISTS `notebooks`;
CREATE TABLE IF NOT EXISTS `notebooks` (
  `id` varchar(40) NOT NULL,
  `ownerid` varchar(40) NOT NULL,
  `public` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `ownerid` (`ownerid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Struttura della tabella `permissions`
--

DROP TABLE IF EXISTS `permissions`;
CREATE TABLE IF NOT EXISTS `permissions` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `notebookid` varchar(40) NOT NULL,
  `usergroupid` varchar(40) NOT NULL,
  `permissions` varchar(30) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `notebook_id` (`notebookid`,`usergroupid`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=660 ;

-- --------------------------------------------------------

--
-- Struttura della tabella `userdata`
--

DROP TABLE IF EXISTS `userdata`;
CREATE TABLE IF NOT EXISTS `userdata` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `userid` varchar(40) NOT NULL,
  `datakey` varchar(20) NOT NULL,
  `data` mediumtext NOT NULL,
  PRIMARY KEY (`id`),
  KEY `userid` (`userid`,`datakey`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=2 ;


DROP TABLE IF EXISTS `emails`;
CREATE TABLE IF NOT EXISTS `emails` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `label` varchar(40) NOT NULL,
  `receivers` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;
--
-- Limiti per le tabelle scaricate
--

--
-- Limiti per la tabella `activenotebooks`
--
ALTER TABLE `activenotebooks`
  ADD CONSTRAINT `activenotebooks_ibfk_1` FOREIGN KEY (`notebookid`) REFERENCES `notebooks` (`id`) ON DELETE CASCADE;

--
-- Limiti per la tabella `annotations`
--
ALTER TABLE `annotations`
  ADD CONSTRAINT `annotations_ibfk_1` FOREIGN KEY (`notebookid`) REFERENCES `notebooks` (`id`) ON DELETE CASCADE;

--
-- Limiti per la tabella `currentnotebooks`
--
ALTER TABLE `currentnotebooks`
  ADD CONSTRAINT `currentnotebooks_ibfk_1` FOREIGN KEY (`currentnotebook`) REFERENCES `notebooks` (`id`) ON DELETE CASCADE;

--
-- Limiti per la tabella `permissions`
--
ALTER TABLE `permissions`
  ADD CONSTRAINT `permissions_ibfk_1` FOREIGN KEY (`notebookid`) REFERENCES `notebooks` (`id`) ON DELETE CASCADE;
